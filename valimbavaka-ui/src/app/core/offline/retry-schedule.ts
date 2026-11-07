import { OutboxEntry } from './outbox-models';

const RETRY_BASE_DELAY_MS = 1_000;
const RETRY_MAX_DELAY_MS = 8_000;

/** Heures des prochaines relances (1, 2, 4 puis 8 s), en mémoire : une relance ne retient que son couloir. */
export class RetrySchedule {
  private readonly retryAt = new Map<string, number>();

  isDue(entry: OutboxEntry, now: number): boolean {
    return (this.retryAt.get(entry.id) ?? 0) <= now;
  }

  schedule(id: string, attempts: number): void {
    const delayMs = Math.min(RETRY_MAX_DELAY_MS, RETRY_BASE_DELAY_MS * 2 ** (attempts - 1));
    this.retryAt.set(id, Date.now() + delayMs);
  }

  /** Une saisie déjà relancée (envoi direct échoué, redémarrage) attend son délai avant le prochain essai. */
  restore(pending: readonly OutboxEntry[]): void {
    pending
      .filter((entry) => entry.attempts > 0 && !this.retryAt.has(entry.id))
      .forEach((entry) => this.schedule(entry.id, entry.attempts));
  }

  /** Délai avant la plus proche relance planifiée, ou null s'il n'y en a aucune. */
  nextDelay(pending: readonly OutboxEntry[], now: number): number | null {
    const delays = pending
      .map((entry) => (this.retryAt.get(entry.id) ?? now) - now)
      .filter((delayMs) => delayMs > 0);
    return delays.length > 0 ? Math.min(...delays) : null;
  }

  forget(id: string): void {
    this.retryAt.delete(id);
  }

  clear(): void {
    this.retryAt.clear();
  }
}
