import { StockLocation } from '../../../shared/data-access/demo-catalog';
import { ProductRange } from '../../../shared/models/tone';

export type { StockLocation } from '../../../shared/data-access/demo-catalog';

/** Stock d'un article par emplacement, calculé par le serveur depuis le journal (STK-01). */
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
