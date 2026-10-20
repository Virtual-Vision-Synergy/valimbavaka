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

/**
 * File d'attente des saisies (pattern outbox) : toute écriture passe par `submit`. Hors connexion elle est
 * persistée puis rejouée dans l'ordre au retour du réseau ou à la connexion. Les refus serveur (4xx) restent
 * visibles (FAILED) sans bloquer les saisies suivantes.
 */
@Injectable({ providedIn: 'root' })
export class OutboxService {
  private readonly http = inject(HttpClient);
  private readonly storage = inject(OutboxStorage);
  private readonly authStore = inject(AuthStore);
  private readonly connectivity = inject(ConnectivityService);

  private readonly entries = signal<OutboxEntry[]>([]);
  private readonly syncing = signal(false);
  private lastStamp = 0;

  readonly isSyncing = this.syncing.asReadonly();
  readonly failed = computed(() => this.entries().filter((entry) => entry.status === 'FAILED'));
  readonly pendingCount = computed(
    () => this.entries().filter((entry) => entry.status === 'PENDING').length,
  );
  readonly failedCount = computed(() => this.failed().length);

  constructor() {
    void this.reload();
    effect(() => {
      if (this.authStore.isAuthenticated() && this.connectivity.isOnline()) {
        untracked(() => void this.flush());
      }
    });
  }

  /** Envoie la saisie, ou la met en file si le réseau est coupé ou si des saisies plus anciennes attendent. */
  submit(method: OutboxMethod, url: string, body?: unknown): Observable<SubmitResult> {
    const entry = this.newEntry(method, url, body);
    if (!this.connectivity.isOnline() || this.pendingCount() > 0) {
      return this.enqueue(entry, true);
    }
    return this.send(entry).pipe(
      map(() => ({ queued: false })),
      catchError((error: unknown) =>
        error instanceof ApiError && error.status === 0
          ? this.enqueue(entry, false)
          : throwError(() => error),
      ),
    );
  }

  /** Rejoue les saisies en attente, de la plus ancienne à la plus récente ; s'arrête si le réseau retombe. */
  async flush(): Promise<void> {
    if (this.syncing()) {
      return;
    }
    this.syncing.set(true);
    try {
      await this.reload();
      for (const entry of this.entries().filter((candidate) => candidate.status === 'PENDING')) {
        if (!(await this.sync(entry))) {
          break;
        }
      }
    } finally {
      this.syncing.set(false);
    }
  }

  async retry(id: string): Promise<void> {
    const entry = this.entries().find((candidate) => candidate.id === id);
    if (entry) {
      await this.storage.put({ ...entry, status: 'PENDING', errorMessage: undefined });
      await this.flush();
    }
  }

  async discard(id: string): Promise<void> {
    await this.storage.remove(id);
    await this.reload();
  }

  /** Vide la file (déconnexion) : les saisies d'un compte ne doivent jamais partir sous un autre. */
  async clear(): Promise<void> {
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

  /** Renvoie false quand la synchronisation doit s'arrêter (réseau, session ou serveur indisponible). */
  private async sync(entry: OutboxEntry): Promise<boolean> {
    try {
      await firstValueFrom(this.send(entry));
      await this.storage.remove(entry.id);
    } catch (error: unknown) {
      if (this.isTransient(error)) {
        return false;
      }
      await this.storage.put({
        ...entry,
        attempts: entry.attempts + 1,
        status: 'FAILED',
        errorMessage: errorMessage(error),
      });
    }
    await this.reload();
    return true;
  }

  private send(entry: OutboxEntry): Observable<unknown> {
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

  private newEntry(method: OutboxMethod, url: string, body: unknown): OutboxEntry {
    this.lastStamp = Math.max(Date.now(), this.lastStamp + 1);
    return {
      id: crypto.randomUUID(),
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
}
