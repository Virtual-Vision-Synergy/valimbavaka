import { ArticleType } from '../../../shared/data-access/demo-catalog';
import { ProductRange } from '../../../shared/models/tone';

export type { ArticleType, StockLocation } from '../../../shared/data-access/demo-catalog';

/** Disponibilité dérivée du stock total et du seuil minimum. */
export type StockStatus = 'AVAILABLE' | 'LOW' | 'OUT';

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
