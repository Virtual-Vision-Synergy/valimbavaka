import { HttpClient } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { Observable, catchError, firstValueFrom, from, map, throwError } from 'rxjs';
import { AuthStore } from '../auth/auth-store';
import { ApiError, errorMessage } from '../http/problem-detail';
import { ConnectivityService } from './connectivity-service';
import { OutboxEntry, OutboxMethod, SubmitResult } from './outbox-models';
import { OutboxStorage } from './outbox-storage';

const IDEMPOTENCY_HEADER = 'Idempotency-Key';
const TRANSIENT_STATUSES = new Set([0, 401, 408, 429]);
const MAX_ATTEMPTS = 5;
const RETRY_BASE_DELAY_MS = 1_000;
const RETRY_MAX_DELAY_MS = 8_000;

type SyncAttemptResult =
  | { readonly type: 'done' }
  | { readonly type: 'stopped' }
  | { readonly type: 'retry'; readonly entry: OutboxEntry };

/**
 * File d'attente des saisies (pattern outbox) : toute écriture passe par `submit`. Hors connexion elle est
 * persistée puis rejouée dans l'ordre avec des relances bornées. Les refus serveur et les relances épuisées
 * restent visibles (FAILED) sans bloquer les saisies suivantes.
 */
@Injectable({ providedIn: 'root' })
export class OutboxService {
  private readonly http = inject(HttpClient);
  private readonly storage = inject(OutboxStorage);
  private readonly authStore = inject(AuthStore);
  private readonly connectivity = inject(ConnectivityService);

  private readonly entries = signal<OutboxEntry[]>([]);
  private readonly syncing = signal(false);
  private readonly accountEntries = computed(() => {
    const userId = this.authStore.currentUser()?.id;
    return userId ? this.entries().filter((entry) => entry.ownerId === userId) : [];
  });
  private lastStamp = 0;
  private syncGeneration = 0;
  private clearGeneration = 0;
  private activeUserId = this.authStore.currentUser()?.id ?? null;
  private cancelRetryDelay: (() => void) | undefined;

  readonly isSyncing = this.syncing.asReadonly();
  readonly failed = computed(() =>
    this.accountEntries().filter((entry) => entry.status === 'FAILED'),
  );
  readonly pendingCount = computed(
    () => this.accountEntries().filter((entry) => entry.status === 'PENDING').length,
  );
  readonly failedCount = computed(() => this.failed().length);

  constructor() {
    void this.reload();
    effect(() => {
      const userId = this.authStore.currentUser()?.id ?? null;
      if (userId !== this.activeUserId) {
        this.activeUserId = userId;
        untracked(() => this.cancelSync());
      }
      if (this.authStore.isAuthenticated() && this.connectivity.isOnline()) {
        untracked(() => void this.flush());
      }
    });
  }

  /** Envoie la saisie, ou la met en file si le réseau est coupé ou si des saisies plus anciennes attendent. */
  submit(method: OutboxMethod, url: string, body?: unknown): Observable<SubmitResult> {
    const ownerId = this.authStore.currentUser()?.id;
    if (!ownerId || !this.authStore.isAuthenticated()) {
      return throwError(
        () => new ApiError(401, 'UNAUTHENTICATED', 'Une session active est requise.'),
      );
    }
    const clearGeneration = this.clearGeneration;
    const entry = this.newEntry(method, url, body, ownerId);
    if (!this.connectivity.isOnline() || this.pendingCount() > 0) {
      return this.enqueue(entry, true);
    }
    return this.send(entry).pipe(
      map(() => ({ queued: false })),
      catchError((error: unknown) =>
        error instanceof ApiError &&
        this.isTransient(error) &&
        clearGeneration === this.clearGeneration
          ? this.enqueue({ ...entry, attempts: 1, errorMessage: errorMessage(error) }, true)
          : throwError(() => error),
      ),
    );
  }

  /** Rejoue les saisies FIFO ; une erreur transitoire déclenche des relances avant la suivante. */
  async flush(): Promise<void> {
    const ownerId = this.authStore.currentUser()?.id;
    if (this.syncing() || !ownerId || !this.canSyncAccount(ownerId)) {
      return;
    }
    const generation = this.syncGeneration;
    this.syncing.set(true);
    try {
      await this.flushAccount(ownerId, generation);
    } finally {
      this.resumeFlushIfNeeded(generation);
    }
  }

  async retry(id: string): Promise<void> {
    const entry = this.accountEntries().find((candidate) => candidate.id === id);
    if (entry) {
      await this.storage.put({
        ...entry,
        attempts: 0,
        status: 'PENDING',
        errorMessage: undefined,
      });
      await this.flush();
    }
  }

  async discard(id: string): Promise<void> {
    if (this.accountEntries().some((entry) => entry.id === id)) {
      await this.storage.remove(id);
      await this.reload();
    }
  }

  /** Vide la file à la demande ; une déconnexion conserve les saisies attribuées à chaque compte. */
  async clear(): Promise<void> {
    this.clearGeneration += 1;
    this.cancelSync();
    await this.storage.clear();
    this.entries.set([]);
  }

  private enqueue(entry: OutboxEntry, flushAfter: boolean): Observable<SubmitResult> {
    const stored = this.storage.put(entry).then(async () => {
      await this.reload();
      if (flushAfter && this.connectivity.isOnline()) {
        void this.flush();
      }
      return { queued: true };
    });
    return from(stored);
  }

  /** Relance les erreurs transitoires avant de passer à la saisie suivante dans la file FIFO. */
  private async sync(entry: OutboxEntry, generation: number): Promise<boolean> {
    let current = entry;
    while (generation === this.syncGeneration) {
      if (!this.canSend(current)) {
        return false;
      }
      if (current.attempts > 0 && !(await this.waitForRetry(current.attempts, generation))) {
        return false;
      }
      if (!this.canSend(current)) {
        return false;
      }
      const result = await this.syncAttempt(current, generation);
      if (result.type === 'done') {
        return true;
      }
      if (result.type === 'stopped') {
        return false;
      }
      current = result.entry;
    }
    return false;
  }

  private async syncAttempt(entry: OutboxEntry, generation: number): Promise<SyncAttemptResult> {
    try {
      if (generation !== this.syncGeneration || !this.canSend(entry)) {
        return { type: 'stopped' };
      }
      await firstValueFrom(this.send(entry));
    } catch (error: unknown) {
      if (generation !== this.syncGeneration || !this.canSend(entry)) {
        return { type: 'stopped' };
      }
      const retry = await this.recordFailure(entry, error);
      return retry ? { type: 'retry', entry: retry } : { type: 'done' };
    }
    if (generation !== this.syncGeneration || !this.canSend(entry)) {
      return { type: 'stopped' };
    }
    await this.storage.remove(entry.id);
    await this.reload();
    return { type: 'done' };
  }

  private async recordFailure(entry: OutboxEntry, error: unknown): Promise<OutboxEntry | null> {
    const attempts = entry.attempts + 1;
    const canRetry = this.isTransient(error) && attempts < MAX_ATTEMPTS;
    const updated: OutboxEntry = {
      ...entry,
      attempts,
      status: canRetry ? 'PENDING' : 'FAILED',
      errorMessage: errorMessage(error),
    };
    await this.storage.put(updated);
    await this.reload();
    return canRetry ? updated : null;
  }

  private waitForRetry(attempts: number, generation: number): Promise<boolean> {
    const delayMs = Math.min(RETRY_MAX_DELAY_MS, RETRY_BASE_DELAY_MS * 2 ** (attempts - 1));
    return new Promise((resolve) => {
      const timeout = setTimeout(() => {
        this.cancelRetryDelay = undefined;
        resolve(generation === this.syncGeneration);
      }, delayMs);
      this.cancelRetryDelay = () => {
        clearTimeout(timeout);
        this.cancelRetryDelay = undefined;
        resolve(false);
      };
    });
  }

  private send(entry: OutboxEntry): Observable<unknown> {
    if (!this.canSend(entry)) {
      return throwError(
        () => new ApiError(401, 'UNAUTHENTICATED', 'Le compte d’origine doit être connecté.'),
      );
    }
    return this.http.request(entry.method, entry.url, {
      body: entry.body,
      headers: { [IDEMPOTENCY_HEADER]: entry.id },
    });
  }

  private isTransient(error: unknown): boolean {
    return (
      error instanceof ApiError && (TRANSIENT_STATUSES.has(error.status) || error.status >= 500)
    );
  }

  private newEntry(method: OutboxMethod, url: string, body: unknown, ownerId: string): OutboxEntry {
    this.lastStamp = Math.max(Date.now(), this.lastStamp + 1);
    return {
      id: crypto.randomUUID(),
      ownerId,
      method,
      url,
      body,
      createdAt: this.lastStamp,
      attempts: 0,
      status: 'PENDING',
    };
  }

  private async reload(): Promise<void> {
    this.entries.set(await this.storage.list());
  }

  private async flushAccount(ownerId: string, generation: number): Promise<void> {
    await this.reload();
    while (generation === this.syncGeneration && this.canSyncAccount(ownerId)) {
      const entry = this.accountEntries().find((candidate) => candidate.status === 'PENDING');
      if (!entry || !(await this.sync(entry, generation))) {
        break;
      }
    }
  }

  private canSend(entry: OutboxEntry): boolean {
    return Boolean(
      this.authStore.isAuthenticated() &&
      entry.ownerId &&
      this.isCurrentAccount(entry.ownerId) &&
      this.connectivity.isOnline(),
    );
  }

  private isCurrentAccount(userId: string): boolean {
    return this.authStore.currentUser()?.id === userId;
  }

  private cancelSync(): void {
    this.syncGeneration += 1;
    this.cancelRetryDelay?.();
  }

  private resumeFlushIfNeeded(generation: number): void {
    this.syncing.set(false);
    const pendingWork = generation !== this.syncGeneration || this.pendingCount() > 0;
    const userId = this.authStore.currentUser()?.id;
    if (userId && this.canSyncAccount(userId) && pendingWork) {
      void this.flush();
    }
  }

  private canSyncAccount(userId: string): boolean {
    return Boolean(
      this.authStore.isAuthenticated() &&
      this.isCurrentAccount(userId) &&
      this.connectivity.isOnline(),
    );
  }
}
