export type OutboxMethod = 'POST' | 'PUT' | 'PATCH' | 'DELETE';

/** PENDING : à envoyer ; FAILED : refusée ou relances épuisées, conservée pour décision de l'utilisateur. */
export type OutboxStatus = 'PENDING' | 'FAILED';

/** Saisie mise en file hors connexion ; `id` sert aussi de clé d'idempotence côté serveur. */
export interface OutboxEntry {
  readonly id: string;
  readonly method: OutboxMethod;
  readonly url: string;
  readonly body: unknown;
  readonly createdAt: number;
  readonly attempts: number;
  readonly status: OutboxStatus;
  readonly errorMessage?: string;
}

export interface SubmitResult {
  /** true si la saisie est en file d'attente et sera synchronisée au retour du réseau. */
  readonly queued: boolean;
}
