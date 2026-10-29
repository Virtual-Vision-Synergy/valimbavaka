import { StockLocation } from '../../../shared/data-access/demo-catalog';
import { ProductRange } from '../../../shared/models/tone';

export type { StockLocation } from '../../../shared/data-access/demo-catalog';

/** Quantité d'un article dans un emplacement ; `code` est le code du référentiel (ex. `STORE`). */
export interface StockLocationQuantityRDto {
  readonly locationId: string;
  readonly code: string;
  readonly label: string;
  readonly quantity: number;
}

/** `GET /api/v1/stocks` : stock d'un article, somme des mouvements par emplacement actif et total (STK-01). */
export interface StockRDto {
  readonly articleId: string;
  readonly code: string;
  readonly name: string;
  readonly rangeId: string | null;
  readonly unit: string;
  readonly threshold: number;
  readonly cost: number | null;
  readonly total: number;
  readonly locations: readonly StockLocationQuantityRDto[];
}

/**
 * Ligne affichée par la page des stocks, déduite d'un `StockRDto` (voir `stock-mapping.ts`).
 * Fournisseur, délai et ventes journalières sont provisoires jusqu'à STK-05.
 */
export interface StockRowRDto {
  readonly articleId: string;
  readonly reference: string;
  readonly name: string;
  readonly range: ProductRange | null;
  readonly quantities: Readonly<Record<StockLocation, number>>;
  readonly threshold: number;
  readonly unitCost: number;
  /** Fournisseur principal ; `null` = fabrication interne (atelier). */
  readonly supplierName: string | null;
  readonly leadTimeDays: number | null;
  readonly dailySales: number;
}
