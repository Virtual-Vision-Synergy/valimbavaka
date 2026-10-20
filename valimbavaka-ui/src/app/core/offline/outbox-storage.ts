import { Injectable } from '@angular/core';
import { OutboxEntry } from './outbox-models';

const DB_NAME = 'vvs-outbox';
const STORE = 'entries';

/**
 * Persistance de la file d'attente dans IndexedDB (survit au rechargement de la page).
 * Sans IndexedDB (navigation privée, tests), la file reste en mémoire pour la session.
 */
@Injectable({ providedIn: 'root' })
export class OutboxStorage {
  private readonly memory = new Map<string, OutboxEntry>();
  private readonly useMemory = typeof indexedDB === 'undefined';
  private db: Promise<IDBDatabase> | null = null;

  /** Entrées triées de la plus ancienne à la plus récente (ordre d'envoi). */
  async list(): Promise<OutboxEntry[]> {
    const entries = this.useMemory
      ? [...this.memory.values()]
      : await this.run<OutboxEntry[]>('readonly', (store) => store.getAll());
    return entries.sort((a, b) => a.createdAt - b.createdAt);
  }

  async put(entry: OutboxEntry): Promise<void> {
    if (this.useMemory) {
      this.memory.set(entry.id, entry);
      return;
    }
    await this.run('readwrite', (store) => store.put(entry));
  }

  async remove(id: string): Promise<void> {
    if (this.useMemory) {
      this.memory.delete(id);
      return;
    }
    await this.run('readwrite', (store) => store.delete(id));
  }

  async clear(): Promise<void> {
    if (this.useMemory) {
      this.memory.clear();
      return;
    }
    await this.run('readwrite', (store) => store.clear());
  }

  private open(): Promise<IDBDatabase> {
    this.db ??= new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, 1);
      request.onupgradeneeded = () => request.result.createObjectStore(STORE, { keyPath: 'id' });
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
    return this.db;
  }

  private async run<T>(
    mode: IDBTransactionMode,
    action: (store: IDBObjectStore) => IDBRequest<T>,
  ): Promise<T> {
    const db = await this.open();
    return new Promise<T>((resolve, reject) => {
      const request = action(db.transaction(STORE, mode).objectStore(STORE));
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
  }
}
