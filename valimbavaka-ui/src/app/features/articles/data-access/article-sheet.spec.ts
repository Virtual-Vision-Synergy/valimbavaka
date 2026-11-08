import { buildOverview, SHEET_RANGES } from './article-sheet-testing';
import { stockStatusOf, toArticleSheet } from './article-sheet';

describe('toArticleSheet', () => {
  it('calcule la marge, la valeur au coût et le statut, et résout la gamme et les emplacements', () => {
    const sheet = toArticleSheet(buildOverview(), SHEET_RANGES);

    expect(sheet.range).toBe('ZEZIKA');
    expect(sheet.margin).toBe(63);
    expect(sheet.value).toBe(36000);
    expect(sheet.status).toBe('LOW');
    expect(sheet.maxQuantity).toBe(10);
    expect(sheet.movements.map((move) => move.location)).toEqual([
      'Boutique',
      'Entrepôt',
      'Entrepôt',
    ]);
    expect(sheet.batches[0]).toMatchObject({
      role: 'OUTPUT',
      product: 'ART-0031 · Savon noir 100 g',
      date: '2026-11-07T07:00:00Z',
    });
    expect(sheet.promotions[0].discount).toBe('−20 %');
  });

  it('laisse marge et valeur vides pour une matière première sans prix ni coût', () => {
    const overview = buildOverview();
    const sheet = toArticleSheet(
      {
        ...overview,
        article: {
          ...overview.article,
          type: 'RAW_MATERIAL',
          price: null,
          cost: null,
          rangeId: null,
        },
        stock: { ...overview.stock, total: 0, locations: [] },
      },
      SHEET_RANGES,
    );

    expect(sheet.margin).toBeNull();
    expect(sheet.value).toBeNull();
    expect(sheet.range).toBeNull();
    expect(sheet.status).toBe('OUT');
    expect(sheet.maxQuantity).toBe(1);
  });

  it('suit la règle de la liste : rupture à zéro, faible sous le seuil, disponible au seuil', () => {
    expect(stockStatusOf(-1, 40)).toBe('OUT');
    expect(stockStatusOf(39.5, 40)).toBe('LOW');
    expect(stockStatusOf(40, 40)).toBe('AVAILABLE');
  });
});
