import { ProductRangeRDto } from '../../referential/data-access/referential-models';
import { toStockRow } from './stock-mapping';
import { StockRDto } from './stock-models';

const RANGES: ProductRangeRDto[] = [
  { id: 'range-1', code: 'ZEZIKA', label: 'Zezika' },
  { id: 'range-2', code: 'BOTANIKA', label: 'Botanika' },
];

const stock: StockRDto = {
  articleId: 'art-1',
  code: 'ART-0031',
  name: 'Savon noir 100 g',
  rangeId: 'range-2',
  unit: 'PIECE',
  threshold: 40,
  cost: 3120.5,
  total: 27,
  locations: [
    { locationId: 'l1', code: 'WAREHOUSE', label: 'Entrepôt', quantity: 20 },
    { locationId: 'l2', code: 'STORE', label: 'Boutique', quantity: 7 },
    { locationId: 'l3', code: 'DELIVERY', label: 'Livraison', quantity: 0 },
    { locationId: 'l4', code: 'TRADE_SHOW', label: 'Salon', quantity: 0 },
  ],
};

describe('toStockRow', () => {
  it('range les quantités du serveur sous les emplacements de l’interface', () => {
    const row = toStockRow(stock, RANGES);

    expect(row.quantities).toEqual({ WAREHOUSE: 20, SHOP: 7, DELIVERY: 0, SALON: 0 });
    expect(row.reference).toBe('ART-0031');
    expect(row.unitCost).toBe(3120.5);
  });

  it('retrouve la gamme par son identifiant', () => {
    expect(toStockRow(stock, RANGES).range).toBe('BOTANIKA');
  });

  it('laisse la gamme vide quand l’article n’en a pas', () => {
    expect(toStockRow({ ...stock, rangeId: null }, RANGES).range).toBeNull();
  });

  it('met zéro aux emplacements absents de la réponse et au coût inconnu', () => {
    const row = toStockRow({ ...stock, cost: null, locations: [] }, RANGES);

    expect(row.quantities).toEqual({ WAREHOUSE: 0, SHOP: 0, DELIVERY: 0, SALON: 0 });
    expect(row.unitCost).toBe(0);
  });

  it('ignore un emplacement inconnu de l’interface', () => {
    const row = toStockRow(
      { ...stock, locations: [{ locationId: 'x', code: 'MOON', label: 'Lune', quantity: 9 }] },
      RANGES,
    );

    expect(row.quantities).toEqual({ WAREHOUSE: 0, SHOP: 0, DELIVERY: 0, SALON: 0 });
  });
});
