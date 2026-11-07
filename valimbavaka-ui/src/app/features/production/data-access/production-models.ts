/**
 * Statut affiché. Le serveur (PRD-01, PRD-03) dit PLANNED / IN_PROGRESS / COMPLETED : voir `toLotStatus`
 * dans `production-mapping.ts` (DRAFT = planifié, CURING = en cours, VALIDATED = terminé).
 */
export type LotStatus = 'DRAFT' | 'CURING' | 'VALIDATED';

/** Miroir de ProductionBatchStatus côté backend. */
export type ProductionBatchStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED';

/** Miroir de ProductionInputWDto : un entrant (MP ou SF) et sa quantité. */
export interface ProductionInputWDto {
  readonly articleId: string;
  readonly quantity: number;
}

/** Miroir de ProductionInputRDto : `unitCost` est le coût d'achat figé à la validation (PRD-02), sinon null. */
export interface ProductionInputRDto extends ProductionInputWDto {
  readonly unitCost?: number | null;
}

/** Miroir de ProductionBatchRDto : lot lu (`GET /api/v1/production/batches`). */
export interface ProductionBatchRDto {
  readonly id: string;
  /** Numéro attribué par le serveur (LOT-00001), jamais saisi. */
  readonly number: string;
  readonly outputArticleId: string;
  readonly quantity: number;
  readonly operatorId: string;
  readonly status: ProductionBatchStatus;
  readonly inputs: readonly ProductionInputRDto[];
  /** Date de validation (PRD-02) : renseignée seulement au statut COMPLETED. */
  readonly completedAt?: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly version: number;
}

/** Miroir de ProductionBatchWDto : création (ni id, ni numéro, ni statut). */
export interface ProductionBatchWDto {
  readonly outputArticleId: string;
  readonly quantity: number;
  readonly operatorId: string;
  readonly inputs: readonly ProductionInputWDto[];
}

/** Miroir de ProductionOperatorRDto : vendeuse active pouvant être responsable d'un lot. */
export interface ProductionOperatorRDto {
  readonly id: string;
  readonly firstName: string;
  readonly lastName: string;
}

/** Responsable proposé dans le formulaire. */
export interface OperatorOption {
  readonly id: string;
  readonly name: string;
}

/** Matière ou emballage consommé par une production. */
export interface MaterialRDto {
  readonly id: string;
  readonly reference: string;
  readonly name: string;
  /** Unité d'affichage et de saisie (kg, g, ml, u.). */
  readonly unit: string;
  readonly available: number;
  /** Coût par unité d'affichage. */
  readonly unitCost: number;
}

/** Quantité de matière nécessaire pour une unité de produit fini. */
export interface RecipeLineRDto {
  readonly materialId: string;
  readonly perUnit: number;
}

export interface ProducibleArticleRDto {
  readonly articleId: string;
  readonly reference: string;
  readonly name: string;
  readonly price: number;
  readonly recipe: readonly RecipeLineRDto[];
}

/** Lot récent (PRD-03). */
export interface LotRDto {
  readonly number: string;
  readonly date: string;
  readonly articleName: string;
  readonly quantity: number;
  readonly operator: string;
  readonly status: LotStatus;
}

export interface ProductionKpisRDto {
  readonly lotsThisMonth: number;
  readonly curing: number;
  readonly unitsProduced: number;
  readonly unitsTrendPercent: number;
  readonly averageCost: number;
  readonly materialsToRebuy: number;
}

/** Contexte de l'écran de production (PRD-01 à PRD-05). */
export interface ProductionContextRDto {
  /** Vide : le numéro est attribué par le serveur à l'enregistrement. */
  readonly nextNumber: string;
  readonly operators: readonly OperatorOption[];
  readonly materials: readonly MaterialRDto[];
  readonly products: readonly ProducibleArticleRDto[];
  readonly recentLots: readonly LotRDto[];
  readonly kpis: ProductionKpisRDto;
}

export interface LotLineWDto {
  readonly materialId: string;
  readonly quantity: number;
}

/**
 * Saisie de l'écran. `validate` : le lot est créé et validé dans la même requête (PRD-02, stock mouvementé),
 * sinon il est créé planifié. Converti en `ProductionBatchWDto` par `toBatchWDto`.
 */
export interface LotWDto {
  readonly articleId: string;
  readonly quantity: number;
  /** Identifiant de la vendeuse responsable. */
  readonly operatorId: string;
  readonly lines: readonly LotLineWDto[];
  readonly validate: boolean;
}
