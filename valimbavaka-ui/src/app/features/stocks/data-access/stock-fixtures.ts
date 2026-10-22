import {
  DEMO_ARTICLES,
  DEMO_SUPPLIERS,
  DemoArticle,
} from '../../../shared/data-access/demo-catalog';
import { StockRowRDto } from './stock-models';

/** Fournisseur principal des articles achetés ; les autres sont fabriqués à l'atelier. */
const SUPPLIER_BY_REFERENCE: Readonly<Record<string, string>> = {
  'ART-0112': 'sup-01',
  'ART-0118': 'sup-01',
  'MP-0012': 'sup-04',
  'MP-0021': 'sup-02',
  'EMB-0004': 'sup-03',
  'EMB-0011': 'sup-05',
  'EMB-0020': 'sup-05',
};

function toRow(article: DemoArticle): StockRowRDto {
  const supplier = DEMO_SUPPLIERS.find(
    (item) => item.id === SUPPLIER_BY_REFERENCE[article.reference],
  );
  return {
    articleId: article.id,
    reference: article.reference,
    name: article.name,
    range: article.range,
    quantities: article.stock,
    threshold: article.threshold,
    unitCost: article.cost,
    supplierName: supplier?.name ?? null,
    leadTimeDays: supplier?.leadTimeDays ?? null,
    dailySales: Math.max(1, Math.round(article.threshold / 6)),
  };
}

export const STOCK_FIXTURE: readonly StockRowRDto[] = DEMO_ARTICLES.map(toRow);
