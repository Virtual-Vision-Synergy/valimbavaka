import { ProductRange } from '../../../shared/models/tone';
import {
  ArticleBatchRDto,
  ArticleBatchRole,
  ArticleOverviewRDto,
  ArticleType,
  ArticleUnit,
} from '../../article/data-access/article-models';
import { ProductionBatchStatus } from '../../production/data-access/production-models';
import { discountLabel } from '../../promotions/data-access/promotion-labels';
import { PromotionRDto } from '../../promotions/data-access/promotion-models';
import { ProductRangeRDto } from '../../referential/data-access/referential-models';
import {
  StockLocationQuantityRDto,
  StockMovementType,
} from '../../stocks/data-access/stock-models';
import { StockStatus } from './article-models';

export interface SheetLocation {
  readonly id: string;
  readonly label: string;
  readonly quantity: number;
}

export interface SheetBatch {
  readonly id: string;
  readonly number: string;
  readonly role: ArticleBatchRole;
  readonly quantity: number;
  readonly status: ProductionBatchStatus;
  /** Produit obtenu par le lot (« ART-0031 · Savon noir »), utile quand l'article est un entrant. */
  readonly product: string;
  /** Date de validation, sinon de création. */
  readonly date: string;
}

export interface SheetPromotion {
  readonly id: string;
  readonly name: string;
  readonly discount: string;
  readonly startsOn: string;
  readonly endsOn: string;
  readonly code: string | null;
  readonly usageCount: number;
}

export interface SheetMovement {
  readonly id: string;
  readonly occurredAt: string;
  readonly type: StockMovementType;
  readonly location: string;
  readonly quantity: number;
}

/** Fiche article 360° (ART-06) prête à afficher : identité, stock par emplacement, lots, promotions, journal. */
export interface ArticleSheet {
  readonly id: string;
  readonly code: string;
  readonly name: string;
  readonly barcode: string | null;
  readonly type: ArticleType;
  readonly unit: ArticleUnit;
  readonly range: ProductRange | null;
  readonly active: boolean;
  readonly price: number | null;
  readonly cost: number | null;
  /** Marge sur le prix de vente en %, arrondie ; null sans prix ou sans coût. */
  readonly margin: number | null;
  readonly threshold: number;
  readonly total: number;
  readonly status: StockStatus;
  /** Stock total au coût d'achat ; null sans coût. */
  readonly value: number | null;
  /** Plus grande quantité d'un emplacement (au moins 1) : longueur des barres. */
  readonly maxQuantity: number;
  readonly locations: readonly SheetLocation[];
  readonly batches: readonly SheetBatch[];
  readonly promotions: readonly SheetPromotion[];
  readonly movements: readonly SheetMovement[];
}

/** Assemble la fiche à partir de la réponse du serveur ; la gamme est résolue avec le référentiel (SYS-05). */
export function toArticleSheet(
  overview: ArticleOverviewRDto,
  ranges: readonly ProductRangeRDto[],
): ArticleSheet {
  const { article, stock } = overview;
  return {
    id: article.id,
    code: article.code,
    name: article.name,
    barcode: article.barcode,
    type: article.type,
    unit: article.unit,
    range: rangeOf(article.rangeId, ranges),
    active: article.active,
    price: article.price,
    cost: article.cost,
    margin: marginOf(article.price, article.cost),
    threshold: article.threshold,
    total: stock.total,
    status: stockStatusOf(stock.total, article.threshold),
    value: article.cost === null ? null : Math.round(stock.total * article.cost),
    maxQuantity: Math.max(1, ...stock.locations.map((location) => location.quantity)),
    locations: stock.locations.map(toSheetLocation),
    batches: overview.productionBatches.map(toSheetBatch),
    promotions: overview.activePromotions.map(toSheetPromotion),
    movements: toSheetMovements(overview),
  };
}

/** Même règle que la liste (ART-03) : rupture à zéro ou moins, stock faible sous le seuil. */
export function stockStatusOf(total: number, threshold: number): StockStatus {
  if (total <= 0) {
    return 'OUT';
  }
  return total < threshold ? 'LOW' : 'AVAILABLE';
}

function marginOf(price: number | null, cost: number | null): number | null {
  if (!price || cost === null) {
    return null;
  }
  return Math.round(((price - cost) / price) * 100);
}

function rangeOf(rangeId: string | null, ranges: readonly ProductRangeRDto[]): ProductRange | null {
  const code = ranges.find((range) => range.id === rangeId)?.code;
  return code === 'BOTANIKA' || code === 'ZEZIKA' || code === 'SOLIKA' ? code : null;
}

function toSheetLocation(location: StockLocationQuantityRDto): SheetLocation {
  return { id: location.locationId, label: location.label, quantity: location.quantity };
}

function toSheetBatch(batch: ArticleBatchRDto): SheetBatch {
  return {
    id: batch.id,
    number: batch.number,
    role: batch.role,
    quantity: batch.quantity,
    status: batch.status,
    product: `${batch.outputArticleCode} · ${batch.outputArticleName}`,
    date: batch.completedAt ?? batch.createdAt,
  };
}

function toSheetPromotion(promotion: PromotionRDto): SheetPromotion {
  return {
    id: promotion.id,
    name: promotion.name,
    discount: discountLabel(promotion.kind, promotion.value),
    startsOn: promotion.startsOn,
    endsOn: promotion.endsOn,
    code: promotion.code,
    usageCount: promotion.usageCount,
  };
}

/** Le libellé de l'emplacement vient du stock de l'article (emplacements actifs du référentiel). */
function toSheetMovements(overview: ArticleOverviewRDto): SheetMovement[] {
  const labels = new Map(overview.stock.locations.map((item) => [item.locationId, item.label]));
  return overview.recentMovements.map((movement) => ({
    id: movement.id,
    occurredAt: movement.occurredAt,
    type: movement.type,
    location: labels.get(movement.locationId) ?? '—',
    quantity: movement.quantity,
  }));
}
