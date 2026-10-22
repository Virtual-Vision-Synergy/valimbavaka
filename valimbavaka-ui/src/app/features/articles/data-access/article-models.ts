import { ArticleType, StockLocation } from '../../../shared/data-access/demo-catalog';
import { ProductRange } from '../../../shared/models/tone';

export type { ArticleType, StockLocation } from '../../../shared/data-access/demo-catalog';

/** Disponibilité dérivée du stock total et du seuil minimum. */
export type StockStatus = 'AVAILABLE' | 'LOW' | 'OUT';

/** Unité de gestion (miroir de l'énumération backend ArticleUnit). */
export type ArticleUnit = 'PIECE' | 'KILOGRAM' | 'LITER';

/** Ligne de la liste des articles (ART-03). */
export interface ArticleRDto {
  readonly id: string;
  readonly reference: string;
  readonly barcode: string;
  readonly name: string;
  readonly type: ArticleType;
  readonly range: ProductRange | null;
  readonly price: number;
  readonly cost: number;
  readonly totalStock: number;
  readonly threshold: number;
  readonly status: StockStatus;
}

export interface LocationStockRDto {
  readonly location: StockLocation;
  readonly quantity: number;
}

export interface ProductionLotRDto {
  readonly number: string;
  readonly quantity: number;
  readonly operator: string;
  readonly unitCost: number | null;
  readonly curingDaysLeft: number | null;
  readonly validated: boolean;
}

export interface ArticlePromotionRDto {
  readonly name: string;
  readonly rule: string;
  readonly period: string;
  readonly code: string;
  readonly uses: number;
  readonly rate: number;
}

export interface ArticleMovementRDto {
  readonly when: string;
  readonly label: string;
  readonly delta: number;
}

/** Fiche article 360° (ART-06) : identité, économie, stock, lots, promotions et journal. */
export interface ArticleDetailRDto extends ArticleRDto {
  readonly description: string;
  readonly manufactured: boolean;
  readonly soldLast30Days: number;
  readonly stockByLocation: readonly LocationStockRDto[];
  readonly lots: readonly ProductionLotRDto[];
  readonly promotion: ArticlePromotionRDto | null;
  readonly endedPromotions: number;
  readonly movements: readonly ArticleMovementRDto[];
}

/** Création d'article (ART-02) : jamais d'identifiant ni de référence (attribués par le serveur). */
export interface ArticleWDto {
  readonly name: string;
  readonly type: ArticleType;
  readonly range: ProductRange | null;
  readonly barcode: string | null;
  readonly manufactured: boolean;
  readonly salonUsable: boolean;
  readonly price: number | null;
  readonly unit: ArticleUnit;
  readonly threshold: number;
  readonly supplierId: string | null;
}

export interface SupplierOptionRDto {
  readonly id: string;
  readonly name: string;
}

/** Valeur brute du formulaire de création (brouillon conservé pendant la saisie). */
export interface ArticleFormValue {
  readonly name: string;
  readonly range: ProductRange | null;
  readonly barcode: string;
  readonly type: ArticleType;
  readonly manufactured: boolean;
  readonly salonUsable: boolean;
  readonly price: number | null;
  readonly unit: ArticleUnit;
  readonly threshold: number | null;
  readonly supplierId: string | null;
}
