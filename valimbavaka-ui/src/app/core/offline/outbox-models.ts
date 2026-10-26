export type OutboxMethod = 'POST' | 'PUT' | 'PATCH' | 'DELETE';

/** PENDING : à envoyer ; FAILED : refusée ou relances épuisées, conservée pour décision de l'utilisateur. */
export type OutboxStatus = 'PENDING' | 'FAILED';

/** Saisie mise en file hors connexion ; `id` sert aussi de clé d'idempotence côté serveur. */
export interface OutboxEntry {
  readonly id: string;
  /** Identifiant du compte créateur ; absent uniquement sur les anciennes entrées non attribuables. */
  readonly ownerId?: string;
  readonly method: OutboxMethod;
  readonly url: string;
  readonly body: unknown;
  readonly createdAt: number;
  readonly attempts: number;
  readonly status: OutboxStatus;
  readonly errorMessage?: string;
}

/**
 * Résultat d'une saisie : `queued` si elle est en file (réseau coupé ou saisies plus anciennes en attente),
 * sinon elle est partie et `body` porte la réponse du serveur (l'entité créée ou modifiée).
 */
export interface SubmitResult<T = unknown> {
  /** true si la saisie est en file d'attente et sera synchronisée au retour du réseau. */
  readonly queued: boolean;
  /** Réponse du serveur ; absente quand la saisie est en file (l'entité n'existe pas encore). */
  readonly body?: T;
}
