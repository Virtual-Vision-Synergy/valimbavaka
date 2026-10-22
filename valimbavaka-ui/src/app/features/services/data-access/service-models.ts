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

/** Prestation enregistrée (SRV-03). */
export interface SalonServiceRDto {
  readonly id: string;
  readonly treatmentName: string;
  readonly time: string;
  readonly staff: string;
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
  readonly staff: readonly string[];
  readonly today: readonly SalonServiceRDto[];
  readonly shootings: readonly ShootingRDto[];
}

/** Enregistrement d'une prestation : le serveur déduit les produits du stock salon. */
export interface SalonServiceWDto {
  readonly treatmentId: string;
  readonly customer: string | null;
  readonly staff: string;
  readonly consumed: readonly { readonly productId: string; readonly quantity: number }[];
}
