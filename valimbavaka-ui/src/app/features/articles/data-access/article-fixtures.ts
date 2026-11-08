import { DEMO_ARTICLES, DemoArticle, totalStock } from '../../../shared/data-access/demo-catalog';
import { ArticleRDto, StockStatus } from './article-models';

function statusOf(total: number, threshold: number): StockStatus {
  if (total === 0) {
    return 'OUT';
  }
  return total < threshold ? 'LOW' : 'AVAILABLE';
}

function toRow(article: DemoArticle): ArticleRDto {
  const total = totalStock(article);
  return {
    id: article.id,
    reference: article.reference,
    barcode: article.barcode,
    name: article.name,
    type: article.type,
    range: article.range,
    price: article.price,
    cost: article.cost,
    totalStock: total,
    threshold: article.threshold,
    status: statusOf(total, article.threshold),
  };
}

export const ARTICLES_FIXTURE: readonly ArticleRDto[] = DEMO_ARTICLES.map(toRow);
