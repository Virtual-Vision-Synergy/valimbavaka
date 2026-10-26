import { ArticleRDto } from '../../data-access/article-models';

export const ARTICLES_URL = '/api/v1/articles';
export const ARTICLE_ID = '0195f0a0-0000-7000-8000-0000000000a1';
export const ARTICLE_URL = `${ARTICLES_URL}/${ARTICLE_ID}`;
export const RANGE_ID = '0195f0a0-0000-7000-8000-0000000000b1';
export const SUPPLIER_ID = '0195f0a0-0000-7000-8000-0000000000c1';

export const article: ArticleRDto = {
  id: ARTICLE_ID,
  code: 'ART-0031',
  name: 'Savon noir 100 g',
  barcode: '3760123456789',
  type: 'FINISHED',
  rangeId: RANGE_ID,
  unit: 'PIECE',
  cost: 3120.5,
  price: 8500,
  threshold: 40,
  supplierId: SUPPLIER_ID,
  active: true,
  createdAt: '2026-10-26T08:30:00Z',
  updatedAt: '2026-10-26T08:30:00Z',
  version: 3,
};

export const problem = (status: number, code: string, detail: string) => ({
  title: code,
  status,
  detail,
  code,
});
