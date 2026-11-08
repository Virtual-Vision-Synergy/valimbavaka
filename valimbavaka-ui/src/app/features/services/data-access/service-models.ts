/** Produit du stock salon consommé par une prestation (unité d'affichage : ml, g, u.). */
export interface SalonProductRDto {
  readonly id: string;
  readonly name: string;
  readonly unit: string;
  readonly stock: number;
}

export interface TreatmentRecipeLineRDto {
  readonly productId: string;
  readonly quantity: number;
}

/** Soin proposé au salon (SRV-01). */
export interface TreatmentRDto {
  readonly id: string;
  readonly name: string;
  readonly minutes: number;
  readonly price: number;
  readonly recipe: readonly TreatmentRecipeLineRDto[];
}

/** Prestation enregistrée (SRV-03), maquette ; à ne pas confondre avec `SalonServiceRDto` (catalogue SRV-01). */
export interface ServiceRecordRDto {
  readonly id: string;
  readonly treatmentName: string;
  readonly time: string;
  readonly staff: string;
  readonly price: number;
}

/** Statut affiché d'une sortie : `LATE` est calculé (encore sortie après le retour prévu). */
export type ShootingStatus = 'OUT' | 'LATE' | 'RETURNED';

/** Ligne affichée d'une sortie : nom de l'article et quantité sortie. */
export interface ShootingRowItemRDto {
  readonly name: string;
  readonly quantity: number;
}

/** Sortie shooting telle qu'affichée par `ShootingList` (SHO-01 à SHO-03), issue de `ShootingRDto`. */
export interface ShootingRowRDto {
  /** Identifiant serveur, pour la réintégration (SHO-02). */
  readonly id: string;
  readonly number: string;
  readonly items: readonly ShootingRowItemRDto[];
  readonly takenBy: string;
  /** Retour prévu, horodatage local `AAAA-MM-JJTHH:MM`. */
  readonly expectedReturn: string;
  readonly status: ShootingStatus;
}

/** Statut enregistré côté serveur : `OUT` à la sortie, `RETURNED` à la réintégration (SHO-02). */
export type ShootingStoredStatus = 'OUT' | 'RETURNED';

/** Article sorti, avec le code et le nom copiés au moment de la sortie. */
export interface ShootingItemRDto {
  readonly articleId: string;
  readonly articleCode: string;
  readonly articleName: string;
  readonly quantity: number;
}

/**
 * `GET /api/v1/shootings` : sortie shooting (SHO-01), réintégrée par `POST /api/v1/shootings/{id}/return` (SHO-02).
 * `late` : encore sortie après `expectedReturnAt`.
 */
export interface ShootingRDto {
  readonly id: string;
  readonly number: string;
  readonly locationId: string;
  readonly items: readonly ShootingItemRDto[];
  readonly takenById: string | null;
  readonly takenBy: string;
  /** Instant ISO 8601 (UTC). */
  readonly expectedReturnAt: string;
  readonly note: string | null;
  readonly status: ShootingStoredStatus;
  readonly late: boolean;
  readonly createdAt: string;
  /** Instant ISO 8601 de la réintégration (SHO-02), `null` tant que la sortie est dehors. */
  readonly returnedAt: string | null;
  /** Nom complet du compte qui a réintégré, `null` tant que la sortie est dehors. */
  readonly returnedBy: string | null;
}

/**
 * `GET /api/v1/shootings/summary` (permission `shootings.track`, SHO-03) : indicateurs des sorties encore dehors.
 * `immobilizedValue` : quantité × coût d'achat, en ariary ; `unvaluedQuantity` : unités sans coût, hors valeur.
 */
export interface ShootingSummaryRDto {
  readonly outCount: number;
  readonly lateCount: number;
  readonly immobilizedValue: number;
  readonly unvaluedQuantity: number;
}

export interface ShootingItemWDto {
  readonly articleId: string;
  readonly quantity: number;
}

/** `POST /api/v1/shootings` : ni numéro, ni statut, ni auteur (attribués par le serveur). */
export interface ShootingWDto {
  readonly locationId: string;
  readonly items: readonly ShootingItemWDto[];
  /** Instant ISO 8601, dans le futur. */
  readonly expectedReturnAt: string;
  readonly note: string | null;
}

export interface ServicesContextRDto {
  readonly treatments: readonly TreatmentRDto[];
  readonly products: readonly SalonProductRDto[];
  readonly staff: readonly string[];
  readonly today: readonly ServiceRecordRDto[];
}

/** Enregistrement d'une prestation : le serveur déduit les produits du stock salon. */
export interface ServiceRecordWDto {
  readonly treatmentId: string;
  readonly customer: string | null;
  readonly staff: string;
  readonly consumed: readonly { readonly productId: string; readonly quantity: number }[];
}
