export type LotStatus = 'DRAFT' | 'CURING' | 'VALIDATED';

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
  readonly nextNumber: string;
  readonly operators: readonly string[];
  readonly materials: readonly MaterialRDto[];
  readonly products: readonly ProducibleArticleRDto[];
  readonly recentLots: readonly LotRDto[];
  readonly kpis: ProductionKpisRDto;
}

export interface LotLineWDto {
  readonly materialId: string;
  readonly quantity: number;
}

/** Création d'un lot : brouillon ou validé (le serveur recalcule le coût de revient). */
export interface LotWDto {
  readonly articleId: string;
  readonly quantity: number;
  readonly operator: string;
  readonly lines: readonly LotLineWDto[];
  readonly validate: boolean;
}
