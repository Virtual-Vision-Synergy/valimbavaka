import { ArticleRDto } from '../../../article/data-access/article-models';
import { ARTICLES_URL, RANGE_ID, article } from '../article-form-page/article-form-page.fixtures';

export { ARTICLES_URL, RANGE_ID };
export const RANGES_URL = '/api/v1/product-ranges';

export const botanika = { id: RANGE_ID, code: 'BOTANIKA', label: 'Botanika' };
export const zezika = {
  id: '0195f0a0-0000-7000-8000-0000000000b2',
  code: 'ZEZIKA',
  label: 'Zezika',
};

export const savonNoir: ArticleRDto = article;
export const huileRicin: ArticleRDto = {
  ...article,
  id: '0195f0a0-0000-7000-8000-0000000000a2',
  code: 'ART-0032',
  name: 'Huile de ricin',
  barcode: null,
  type: 'RAW_MATERIAL',
  rangeId: null,
  price: null,
  active: false,
};

export const pageOf = (items: ArticleRDto[], page = 1, totalCount = items.length) => ({
  items,
  page,
  pageSize: 10,
  totalCount,
  totalPages: Math.max(1, Math.ceil(totalCount / 10)),
});
