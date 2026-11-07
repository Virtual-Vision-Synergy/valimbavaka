import { HttpClient } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { Observable, catchError, firstValueFrom, from, map, throwError } from 'rxjs';
import { AuthStore } from '../auth/auth-store';
import { ApiError, errorMessage } from '../http/problem-detail';
import { ConnectivityService } from './connectivity-service';
import { OutboxEntry, OutboxMethod, SubmitResult } from './outbox-models';
import { OutboxStorage } from './outbox-storage';
import { RetrySchedule } from './retry-schedule';
import { nextSendableEntry } from './sync-order';

const IDEMPOTENCY_HEADER = 'Idempotency-Key';
const TRANSIENT_STATUSES = new Set([0, 401, 408, 429]);
const MAX_ATTEMPTS = 5;

/**
 * File d'attente des saisies (pattern outbox) : toute écriture passe par `submit`. Hors connexion elle est
 * persistée puis rejouée dans l'ordre avec des relances bornées. L'ordre en cas de blocage suit la règle de
 * SYS-10 (`sync-order.ts`) : les opérations attendent derrière une saisie bloquée par le réseau, les fiches
 * indépendantes passent devant. Les refus serveur restent visibles (FAILED) sans bloquer les suivantes.
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
  private endPause: ((proceed: boolean) => void) | undefined;
  private readonly retries = new RetrySchedule();

  readonly isSyncing = this.syncing.asReadonly();
  readonly failed = computed(() =>
    this.accountEntries().filter((entry) => entry.status === 'FAILED'),
  );
  /** Saisies du compte encore à envoyer, dans l'ordre de la file (SYS-09 : affichées en lecture seule). */
  readonly pending = computed(() =>
    this.accountEntries().filter((entry) => entry.status === 'PENDING'),
  );
  readonly pendingCount = computed(() => this.pending().length);
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
  submit<T = unknown>(
    method: OutboxMethod,
    url: string,
    body?: unknown,
  ): Observable<SubmitResult<T>> {
    const ownerId = this.authStore.currentUser()?.id;
    if (!ownerId || !this.authStore.isAuthenticated()) {
      return throwError(
        () => new ApiError(401, 'UNAUTHENTICATED', 'Une session active est requise.'),
      );
    }
    const clearGeneration = this.clearGeneration;
    const entry = this.newEntry(method, url, body, ownerId);
    if (!this.connectivity.isOnline() || this.pendingCount() > 0) {
      return this.enqueue<T>(entry, true);
    }
    return this.send(entry).pipe(
      map((response) => ({ queued: false, body: response as T })),
      catchError((error: unknown) =>
        error instanceof ApiError &&
        this.isTransient(error) &&
        clearGeneration === this.clearGeneration
          ? this.enqueue<T>({ ...entry, attempts: 1, errorMessage: errorMessage(error) }, true)
          : throwError(() => error),
      ),
    );
  }

  /** Rejoue les saisies dans l'ordre ; une saisie en attente de relance ne retient que son couloir (SYS-10). */
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

  /**
   * Remet une saisie FAILED en attente (compteur remis à zéro) puis lance le rejeu. L'état est rechargé avant
   * le rejeu pour que l'écran la montre « en attente » même hors ligne ou pendant une synchronisation (SYS-09).
   */
  async retry(id: string): Promise<void> {
    const entry = this.failed().find((candidate) => candidate.id === id);
    if (entry) {
      await this.storage.put({
        ...entry,
        attempts: 0,
        status: 'PENDING',
        errorMessage: undefined,
        networkFailure: undefined,
      });
      await this.reload();
      this.endPause?.(true);
      await this.flush();
    }
  }

  /** Supprime définitivement une saisie du compte ; l'écran demande une confirmation avant (SYS-09). */
  async discard(id: string): Promise<void> {
    if (this.accountEntries().some((entry) => entry.id === id)) {
      this.retries.forget(id);
      await this.storage.remove(id);
      await this.reload();
      this.endPause?.(true);
      void this.flush();
    }
  }

  /** Vide la file à la demande ; une déconnexion conserve les saisies attribuées à chaque compte. */
  async clear(): Promise<void> {
    this.clearGeneration += 1;
    this.cancelSync();
    await this.storage.clear();
    this.retries.clear();
    this.entries.set([]);
  }

  private enqueue<T>(entry: OutboxEntry, flushAfter: boolean): Observable<SubmitResult<T>> {
    const stored = this.storage.put(entry).then(async () => {
      await this.reload();
      this.endPause?.(true);
      if (flushAfter && this.connectivity.isOnline()) {
        void this.flush();
      }
      return { queued: true } as SubmitResult<T>;
    });
    return from(stored);
  }

  /** Un seul essai ; en erreur transitoire, la relance est planifiée et les autres couloirs continuent. */
  private async syncOnce(entry: OutboxEntry, generation: number): Promise<boolean> {
    try {
      if (!this.isActive(entry, generation)) {
        return false;
      }
      await firstValueFrom(this.send(entry));
    } catch (error: unknown) {
      if (!this.isActive(entry, generation)) {
        return false;
      }
      await this.recordFailure(entry, error);
      return true;
    }
    if (!this.isActive(entry, generation)) {
      return false;
    }
    this.retries.forget(entry.id);
    await this.storage.remove(entry.id);
    await this.reload();
    return true;
  }

  private async recordFailure(entry: OutboxEntry, error: unknown): Promise<void> {
    const attempts = entry.attempts + 1;
    const transient = this.isTransient(error);
    const canRetry = transient && attempts < MAX_ATTEMPTS;
    this.retries.forget(entry.id);
    if (canRetry) {
      this.retries.schedule(entry.id, attempts);
    }
    await this.storage.put({
      ...entry,
      attempts,
      status: canRetry ? 'PENDING' : 'FAILED',
      errorMessage: errorMessage(error),
      networkFailure: transient && !canRetry,
    });
    await this.reload();
  }

  /** Attend la prochaine relance ; une nouvelle saisie, un renvoi ou un abandon réveille la boucle. */
  private pause(delayMs: number, generation: number): Promise<boolean> {
    return new Promise((resolve) => {
      const timeout = setTimeout(() => this.endPause?.(true), delayMs);
      this.endPause = (proceed) => {
        clearTimeout(timeout);
        this.endPause = undefined;
        resolve(proceed && generation === this.syncGeneration);
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
      this.retries.restore(this.pending());
      const entry = this.nextSendable();
      if (entry) {
        if (!(await this.syncOnce(entry, generation))) {
          break;
        }
        continue;
      }
      const delayMs = this.retries.nextDelay(this.pending(), Date.now());
      if (delayMs === null || !(await this.pause(delayMs, generation))) {
        break;
      }
    }
  }

  private nextSendable(): OutboxEntry | undefined {
    const now = Date.now();
    return nextSendableEntry(this.accountEntries(), (entry) => this.retries.isDue(entry, now));
  }

  private isActive(entry: OutboxEntry, generation: number): boolean {
    return generation === this.syncGeneration && this.canSend(entry);
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
    this.endPause?.(false);
  }

  private resumeFlushIfNeeded(generation: number): void {
    this.syncing.set(false);
    const pendingWork =
      generation !== this.syncGeneration ||
      this.nextSendable() !== undefined ||
      this.retries.nextDelay(this.pending(), Date.now()) !== null;
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
