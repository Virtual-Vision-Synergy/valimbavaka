/** Produit du stock salon consommé par une prestation : article actif, unité courte (`u.`, `kg`, `L`), stock salon. */
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

/** Soin proposé au salon : service actif du catalogue (SRV-01), recette = produits habituellement consommés. */
export interface TreatmentRDto {
  readonly id: string;
  readonly name: string;
  readonly minutes: number;
  readonly price: number;
  readonly recipe: readonly TreatmentRecipeLineRDto[];
}

/** Produit consommé affiché dans l'historique du jour : nom copié, quantité et unité courte de l'article. */
export interface ServiceRecordRowProductRDto {
  readonly name: string;
  readonly quantity: number;
  readonly unit: string;
}

/**
 * Ligne « Mes prestations du jour » (SRV-03), déduite d'un `ServiceRecordRDto` (`service-record-mapping.ts`) :
 * heure, service, cliente et produits consommés. À ne pas confondre avec `SalonServiceRDto` (catalogue SRV-01).
 */
export interface ServiceRecordRowRDto {
  readonly id: string;
  readonly number: string;
  readonly treatmentName: string;
  readonly time: string;
  readonly customer: string | null;
  readonly products: readonly ServiceRecordRowProductRDto[];
  readonly price: number;
}

export type ShootingStatus = 'OUT' | 'LATE' | 'RETURNED';

export interface ShootingItemRDto {
  readonly name: string;
  readonly quantity: number;
}

/** Sortie de produits pour un shooting photo (SHO-01 à SHO-03). */
export interface ShootingRDto {
  readonly number: string;
  readonly items: readonly ShootingItemRDto[];
  readonly takenBy: string;
  /** Retour prévu, horodatage local `AAAA-MM-JJTHH:MM`. */
  readonly expectedReturn: string;
  readonly status: ShootingStatus;
}

export interface ServicesContextRDto {
  readonly treatments: readonly TreatmentRDto[];
  readonly products: readonly SalonProductRDto[];
  readonly today: readonly ServiceRecordRowRDto[];
  readonly shootings: readonly ShootingRDto[];
}
