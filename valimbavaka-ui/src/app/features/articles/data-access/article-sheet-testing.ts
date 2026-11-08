import { ArticleOverviewRDto, ArticleRDto } from '../../article/data-access/article-models';
import { buildPromotion } from '../../promotions/data-access/promotion-testing';
import { ProductRangeRDto } from '../../referential/data-access/referential-models';

/** Données de test de la fiche 360° (ART-06) : un savon Zezika produit par un lot, une promotion, 3 mouvements. */
export const SHEET_RANGES: ProductRangeRDto[] = [
  { id: 'range-zezika', code: 'ZEZIKA', label: 'Zezika' },
];

const SOAP: ArticleRDto = {
  id: 'article-soap',
  code: 'ART-0031',
  name: 'Savon noir 100 g',
  barcode: '6131234567890',
  type: 'FINISHED',
  rangeId: 'range-zezika',
  unit: 'PIECE',
  cost: 3000,
  price: 8000,
  threshold: 40,
  supplierId: null,
  active: true,
  createdAt: '2026-11-01T08:00:00Z',
  updatedAt: '2026-11-01T08:00:00Z',
  version: 0,
};

const SOAP_STOCK: ArticleOverviewRDto['stock'] = {
  articleId: SOAP.id,
  code: SOAP.code,
  name: SOAP.name,
  rangeId: SOAP.rangeId,
  unit: SOAP.unit,
  threshold: SOAP.threshold,
  cost: SOAP.cost,
  total: 12,
  locations: [
    { locationId: 'loc-warehouse', code: 'WAREHOUSE', label: 'Entrepôt', quantity: 10 },
    { locationId: 'loc-store', code: 'STORE', label: 'Boutique', quantity: 2 },
  ],
};

const SOAP_BATCH: ArticleOverviewRDto['productionBatches'][number] = {
  id: 'batch-12',
  number: 'LOT-00012',
  status: 'COMPLETED',
  role: 'OUTPUT',
  quantity: 12,
  outputArticleId: SOAP.id,
  outputArticleCode: SOAP.code,
  outputArticleName: SOAP.name,
  createdAt: '2026-11-07T06:00:00Z',
  completedAt: '2026-11-07T07:00:00Z',
};

export function buildOverview(overrides: Partial<ArticleOverviewRDto> = {}): ArticleOverviewRDto {
  return {
    article: SOAP,
    stock: SOAP_STOCK,
    activePromotions: [buildPromotion()],
    productionBatches: [SOAP_BATCH],
    recentMovements: [
      movement('move-3', 'loc-store', 'TRANSFER', 2),
      movement('move-2', 'loc-warehouse', 'TRANSFER', -2),
      movement('move-1', 'loc-warehouse', 'PRODUCTION', 12),
    ],
    ...overrides,
  };
}

function movement(
  id: string,
  locationId: string,
  type: 'TRANSFER' | 'PRODUCTION',
  quantity: number,
): ArticleOverviewRDto['recentMovements'][number] {
  return {
    id,
    articleId: SOAP.id,
    locationId,
    type,
    quantity,
    referenceId: null,
    occurredAt: '2026-11-07T07:00:00Z',
  };
}
