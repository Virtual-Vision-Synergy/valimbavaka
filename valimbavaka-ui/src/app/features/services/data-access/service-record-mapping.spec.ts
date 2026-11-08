import { StockRDto } from '../../stocks/data-access/stock-models';
import { SalonServiceRDto } from './salon-catalog-models';
import { ServiceRecordRDto } from './service-record-models';
import { toSalonProduct, toServiceRow, toTreatment } from './service-record-mapping';

const STOCK: StockRDto = {
  articleId: 'huile-coco',
  code: 'ART-0001',
  name: 'Huile de coco vierge',
  rangeId: null,
  unit: 'LITER',
  threshold: 1,
  cost: null,
  total: 3.2,
  locations: [
    { locationId: 'l1', code: 'STORE', label: 'Boutique', quantity: 3 },
    { locationId: 'l2', code: 'TRADE_SHOW', label: 'Salon', quantity: 0.2 },
  ],
};

const SERVICE: SalonServiceRDto = {
  id: 'soin-visage',
  name: 'Soin visage éclat',
  durationMinutes: 45,
  price: 35000,
  commissionRate: null,
  products: [
    { articleId: 'huile-coco', quantity: 0.015 },
    { articleId: 'article-desactive', quantity: 1 },
  ],
  active: true,
  createdAt: '2026-11-01T08:00:00Z',
  updatedAt: '2026-11-01T08:00:00Z',
  version: 0,
};

describe('service-record-mapping', () => {
  it('propose un article avec son unité courte et sa quantité dans le stock salon', () => {
    expect(toSalonProduct(STOCK)).toEqual({
      id: 'huile-coco',
      name: 'Huile de coco vierge',
      unit: 'L',
      stock: 0.2,
    });
  });

  it("compte 0 en salon pour un article absent de l'emplacement salon", () => {
    expect(toSalonProduct({ ...STOCK, locations: [] }).stock).toBe(0);
  });

  it('pré-remplit la recette du catalogue sans les articles qui ne sont plus proposés', () => {
    const treatment = toTreatment(SERVICE, new Set(['huile-coco']));

    expect(treatment).toEqual({
      id: 'soin-visage',
      name: 'Soin visage éclat',
      minutes: 45,
      price: 35000,
      recipe: [{ productId: 'huile-coco', quantity: 0.015 }],
    });
  });

  it("affiche une prestation à l'heure locale avec le soin, la cliente et les produits consommés", () => {
    const performedAt = new Date(2026, 10, 8, 10, 22).toISOString();
    const record: ServiceRecordRDto = {
      id: 'p1',
      number: 'PRE-0001',
      salonServiceId: 'soin-visage',
      serviceName: 'Soin visage éclat',
      durationMinutes: 45,
      price: 35000,
      customerName: 'Hanta',
      sellerId: 'u1',
      sellerName: 'Miora Andriamasy',
      salesChannelId: 'c1',
      locationId: 'l2',
      products: [
        {
          articleId: 'huile-coco',
          articleCode: 'ART-0001',
          articleName: 'Huile de coco',
          quantity: 0.015,
        },
        {
          articleId: 'ancien',
          articleCode: 'ART-0009',
          articleName: 'Article retiré',
          quantity: 1,
        },
      ],
      note: null,
      performedAt,
    };

    expect(toServiceRow(record, new Map([['huile-coco', 'L']]))).toEqual({
      id: 'p1',
      number: 'PRE-0001',
      treatmentName: 'Soin visage éclat',
      time: '10:22',
      customer: 'Hanta',
      products: [
        { name: 'Huile de coco', quantity: 0.015, unit: 'L' },
        { name: 'Article retiré', quantity: 1, unit: '' },
      ],
      price: 35000,
    });
  });
});
