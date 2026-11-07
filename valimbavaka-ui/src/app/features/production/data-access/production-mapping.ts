import { ArticleRDto, ArticleUnit } from '../../article/data-access/article-models';
import { StockRDto } from '../../stocks/data-access/stock-models';
import {
  LotRDto,
  LotStatus,
  LotWDto,
  MaterialRDto,
  OperatorOption,
  ProducibleArticleRDto,
  ProductionBatchRDto,
  ProductionBatchStatus,
  ProductionBatchWDto,
  ProductionIndicatorsRDto,
  ProductionKpisRDto,
  ProductionOperatorRDto,
} from './production-models';

/** Emplacement où la production consomme et range (hypothèse PRD-01, reprise par PRD-02). */
export const PRODUCTION_LOCATION = 'WAREHOUSE';

const UNIT_LABELS: Readonly<Record<ArticleUnit, string>> = {
  PIECE: 'u.',
  KILOGRAM: 'kg',
  LITER: 'l',
};

const LOT_STATUSES: Readonly<Record<ProductionBatchStatus, LotStatus>> = {
  PLANNED: 'DRAFT',
  IN_PROGRESS: 'CURING',
  COMPLETED: 'VALIDATED',
};

export function toLotStatus(status: ProductionBatchStatus): LotStatus {
  return LOT_STATUSES[status];
}

/** Produit fini proposé comme produit obtenu ; aucune recette côté serveur, les entrants se choisissent. */
export function toProducible(article: ArticleRDto): ProducibleArticleRDto {
  return {
    articleId: article.id,
    reference: article.code,
    name: article.name,
    price: article.price ?? 0,
    recipe: [],
  };
}

/** Entrant proposé ; `available` = stock de l'entrepôt (0 si l'article n'a pas de ligne de stock). */
export function toMaterial(article: ArticleRDto, stocks: readonly StockRDto[]): MaterialRDto {
  const stock = stocks.find((item) => item.articleId === article.id);
  const warehouse = stock?.locations.find((location) => location.code === PRODUCTION_LOCATION);
  return {
    id: article.id,
    reference: article.code,
    name: article.name,
    unit: UNIT_LABELS[article.unit],
    available: warehouse?.quantity ?? 0,
    unitCost: article.cost ?? 0,
  };
}

export function toOperator(operator: ProductionOperatorRDto): OperatorOption {
  return { id: operator.id, name: `${operator.firstName} ${operator.lastName}` };
}

/** Ligne « Lots récents » ; un article ou une vendeuse hors des listes chargées s'affiche « — ». */
export function toLot(
  batch: ProductionBatchRDto,
  products: readonly ProducibleArticleRDto[],
  operators: readonly OperatorOption[],
): LotRDto {
  return {
    id: batch.id,
    number: batch.number,
    date: batch.createdAt.slice(0, 10),
    articleName: products.find((item) => item.articleId === batch.outputArticleId)?.name ?? '—',
    quantity: batch.quantity,
    operator: operators.find((item) => item.id === batch.operatorId)?.name ?? '—',
    status: toLotStatus(batch.status),
  };
}

/** Saisie de l'écran vers le contrat serveur : les lignes à quantité nulle ne sont pas envoyées. */
export function toBatchWDto(lot: LotWDto): ProductionBatchWDto {
  return {
    outputArticleId: lot.articleId,
    quantity: lot.quantity,
    operatorId: lot.operatorId,
    inputs: lot.lines
      .filter((line) => line.quantity > 0)
      .map((line) => ({ articleId: line.materialId, quantity: line.quantity })),
  };
}

/**
 * Indicateurs de tête (PRD-04) : ceux du serveur, plus les entrants à racheter, c'est-à-dire les entrants dont le
 * stock total est sous un seuil non nul (même règle que les alertes STK-04).
 */
export function toKpis(
  indicators: ProductionIndicatorsRDto,
  inputIds: readonly string[],
  stocks: readonly StockRDto[],
): ProductionKpisRDto {
  return {
    lotsThisMonth: indicators.completedBatches,
    curing: indicators.inProgressBatches,
    unitsProduced: indicators.unitsProduced,
    unitsTrendPercent: indicators.unitsTrendPercent ?? null,
    averageCost: indicators.materialCostPerUnit ?? null,
    uncostedLots: indicators.uncostedBatches,
    materialsToRebuy: stocks.filter(
      (stock) =>
        inputIds.includes(stock.articleId) && stock.threshold > 0 && stock.total < stock.threshold,
    ).length,
  };
}
