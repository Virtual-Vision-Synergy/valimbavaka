import { CurrentUserRDto } from '../../../core/auth/auth-models';
import { ProductRangeRDto } from '../../referential/data-access/referential-models';
import { toPosContext, toSaleWDto } from './pos-mapping';
import { PosArticleRDto, SaleFormRDto } from './pos-models';

const RANGES: ProductRangeRDto[] = [{ id: 'range-botanika', code: 'BOTANIKA', label: 'Botanika' }];

const FORM: SaleFormRDto = {
  nextNumber: 42,
  channels: [
    { id: 'channel-store', code: 'STORE', label: 'Boutique', stockLocationId: 'location-store' },
    {
      id: 'channel-facebook',
      code: 'FACEBOOK',
      label: 'Facebook',
      stockLocationId: 'location-store',
    },
    {
      id: 'channel-delivery',
      code: 'DELIVERY',
      label: 'Livraison',
      stockLocationId: 'location-delivery',
    },
  ],
  sellers: [
    { id: 'seller-miora', firstName: 'Miora', lastName: 'Rakoto', salesChannelId: 'channel-store' },
    { id: 'seller-tiana', firstName: 'Tiana', lastName: 'Ranaivo', salesChannelId: null },
  ],
  articles: [
    {
      id: 'soap',
      code: 'ART-0031',
      barcode: null,
      name: 'Savon noir 100 g',
      rangeId: 'range-botanika',
      price: 8500,
      stock: { 'location-store': 12, 'location-delivery': 3 },
    },
    {
      id: 'oil',
      code: 'ART-0040',
      barcode: '3760000000017',
      name: 'Huile de coco',
      rangeId: 'range-botanika',
      price: 12000,
      stock: {},
    },
  ],
  promotions: [
    {
      id: 'promo-rose',
      name: 'Octobre rose',
      kind: 'PERCENT',
      value: 15,
      articleIds: ['soap'],
      channelIds: ['channel-store', 'channel-delivery'],
    },
  ],
};

const SELLER: CurrentUserRDto = {
  id: 'seller-miora',
  email: 'miora@valimbavaka.mg',
  firstName: 'Miora',
  lastName: 'Rakoto',
  role: 'SELLER',
  roleLabel: 'Vendeuse',
  permissions: ['sales.create'],
  passwordChangeRequired: false,
};

describe('toPosContext', () => {
  it('affiche le stock de la boutique, les vendeuses et les canaux du poste', () => {
    const context = toPosContext(FORM, RANGES, SELLER, new Date(2026, 10, 8, 9, 5));

    expect(context.location).toBe('Boutique');
    expect(context.cashierName).toBe('Miora Rakoto');
    expect(context.ticketNumber).toBe(42);
    expect(context.openedAt).toBe('09:05');
    expect(context.channelIds).toEqual({ SHOP: 'channel-store', FACEBOOK: 'channel-facebook' });
    expect(context.sellers).toEqual([
      { id: 'seller-miora', name: 'Miora Rakoto' },
      { id: 'seller-tiana', name: 'Tiana Ranaivo' },
    ]);
    expect(context.defaultSellerId).toBe('seller-miora');
    expect(context.articles.map((article) => [article.id, article.stock, article.range])).toEqual([
      ['soap', 12, 'BOTANIKA'],
      ['oil', 0, 'BOTANIKA'],
    ]);
  });

  it('traduit les canaux des promotions en canaux du poste et écarte ceux que le poste ne vend pas', () => {
    const context = toPosContext(FORM, RANGES, SELLER, new Date());

    expect(context.promotions).toEqual([
      {
        id: 'promo-rose',
        name: 'Octobre rose',
        kind: 'PERCENT',
        value: 15,
        articleIds: ['soap'],
        channels: ['SHOP'],
      },
    ]);
  });

  it('ne propose aucune vendeuse d’office à un administrateur', () => {
    const admin = { ...SELLER, id: 'admin', role: 'ADMIN' };

    expect(toPosContext(FORM, RANGES, admin, new Date()).defaultSellerId).toBeNull();
  });

  it('écarte un article dont la gamme est inconnue', () => {
    expect(toPosContext(FORM, [], SELLER, new Date()).articles).toEqual([]);
  });
});

describe('toSaleWDto', () => {
  it('envoie canal, vendeuse, règlement et quantités sans aucun prix', () => {
    const soap = { id: 'soap', price: 8500 } as PosArticleRDto;

    const body = toSaleWDto('channel-store', 'seller-miora', 'MVOLA', [
      { article: soap, quantity: 3 },
    ]);

    expect(body).toEqual({
      salesChannelId: 'channel-store',
      sellerId: 'seller-miora',
      paymentMethod: 'MVOLA',
      lines: [{ articleId: 'soap', quantity: 3 }],
    });
  });
});
