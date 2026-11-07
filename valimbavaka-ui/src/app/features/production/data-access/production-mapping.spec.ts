import { ArticleRDto } from '../../article/data-access/article-models';
import { StockRDto } from '../../stocks/data-access/stock-models';
import { toBatchWDto, toLot, toLotStatus, toMaterial, toProducible } from './production-mapping';
import { ProductionBatchRDto } from './production-models';

const article = (overrides: Partial<ArticleRDto>): ArticleRDto => ({
  id: 'art-1',
  code: 'ART-0008',
  name: 'Base savonneuse',
  barcode: null,
  type: 'RAW_MATERIAL',
  rangeId: null,
  unit: 'KILOGRAM',
  cost: 12000,
  price: null,
  threshold: 5,
  supplierId: null,
  active: true,
  createdAt: '2026-11-08T08:00:00Z',
  updatedAt: '2026-11-08T08:00:00Z',
  version: 0,
  ...overrides,
});

const stock: StockRDto = {
  articleId: 'art-1',
  code: 'ART-0008',
  name: 'Base savonneuse',
  rangeId: null,
  unit: 'KILOGRAM',
  threshold: 5,
  cost: 12000,
  total: 15.5,
  locations: [
    { locationId: 'l1', code: 'WAREHOUSE', label: 'Entrepôt', quantity: 12.5 },
    { locationId: 'l2', code: 'STORE', label: 'Boutique', quantity: 3 },
  ],
};

const batch: ProductionBatchRDto = {
  id: 'batch-1',
  number: 'LOT-00007',
  outputArticleId: 'pf-1',
  quantity: 30,
  operatorId: 'op-1',
  status: 'PLANNED',
  inputs: [{ articleId: 'art-1', quantity: 2.4 }],
  createdAt: '2026-11-08T09:30:00Z',
  updatedAt: '2026-11-08T09:30:00Z',
  version: 0,
};

describe('production-mapping', () => {
  it("prend le disponible de l'entrepôt, l'unité courte et le coût de l'article", () => {
    expect(toMaterial(article({}), [stock])).toEqual({
      id: 'art-1',
      reference: 'ART-0008',
      name: 'Base savonneuse',
      unit: 'kg',
      available: 12.5,
      unitCost: 12000,
    });
  });

  it('met le disponible et le coût à zéro sans ligne de stock ni coût', () => {
    const material = toMaterial(article({ id: 'art-2', cost: null }), [stock]);

    expect(material.available).toBe(0);
    expect(material.unitCost).toBe(0);
  });

  it("propose un produit fini sans recette, au prix de l'article", () => {
    const product = toProducible(article({ id: 'pf-1', type: 'FINISHED', price: 8500 }));

    expect(product.price).toBe(8500);
    expect(product.recipe).toEqual([]);
  });

  it('affiche un lot planifié comme planifié (DRAFT) avec ses libellés', () => {
    const lot = toLot(
      batch,
      [{ articleId: 'pf-1', reference: 'ART-0031', name: 'Savon noir', price: 8500, recipe: [] }],
      [{ id: 'op-1', name: 'Soa Rakoto' }],
    );

    expect(lot).toEqual({
      number: 'LOT-00007',
      date: '2026-11-08',
      articleName: 'Savon noir',
      quantity: 30,
      operator: 'Soa Rakoto',
      status: 'DRAFT',
    });
  });

  it('affiche « — » pour un article ou une vendeuse hors des listes', () => {
    const lot = toLot(batch, [], []);

    expect(lot.articleName).toBe('—');
    expect(lot.operator).toBe('—');
  });

  it('traduit les trois statuts du serveur', () => {
    expect(toLotStatus('PLANNED')).toBe('DRAFT');
    expect(toLotStatus('IN_PROGRESS')).toBe('CURING');
    expect(toLotStatus('COMPLETED')).toBe('VALIDATED');
  });

  it("envoie les entrants au format du serveur, sans les lignes à zéro ni l'indicateur de validation", () => {
    const body = toBatchWDto({
      articleId: 'pf-1',
      quantity: 30,
      operatorId: 'op-1',
      lines: [
        { materialId: 'art-1', quantity: 2.4 },
        { materialId: 'art-2', quantity: 0 },
      ],
      validate: true,
    });

    expect(body).toEqual({
      outputArticleId: 'pf-1',
      quantity: 30,
      operatorId: 'op-1',
      inputs: [{ articleId: 'art-1', quantity: 2.4 }],
    });
  });
});
