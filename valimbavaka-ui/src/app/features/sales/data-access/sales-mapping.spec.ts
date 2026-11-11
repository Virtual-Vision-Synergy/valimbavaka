import {
  articlesLabel,
  localDateTime,
  localIsoDate,
  toInvoicePreview,
  toInvoiceView,
  toSaleRow,
} from './sales-mapping';
import { InvoiceRDto, SaleSummaryRDto } from './sales-models';

const SALE: SaleSummaryRDto = {
  id: 'vente-42',
  number: 'VTE-0042',
  businessDate: '2026-11-08',
  createdAt: '2026-11-08T07:52:00Z',
  salesChannelId: 'canal-boutique',
  salesChannelLabel: 'Boutique',
  sellerId: 'miora',
  sellerName: 'Miora Rakoto',
  paymentMethod: 'MVOLA',
  status: 'VALIDATED',
  subtotal: 29000,
  discountAmount: 0,
  total: 29000,
  lines: [
    { articleId: 'savon', articleName: 'Savon coco', quantity: 2, lineTotal: 17000 },
    { articleId: 'huile', articleName: 'Huile de ricin', quantity: 1, lineTotal: 12000 },
  ],
};

const INVOICE: InvoiceRDto = {
  id: 'facture-1',
  number: 'FAC-2026-0042',
  saleId: 'vente-42',
  saleNumber: 'VTE-0042',
  issuedAt: '2026-11-08T07:52:00Z',
  businessDate: '2026-11-08',
  status: 'VALIDATED',
  salesChannelLabel: 'Boutique',
  sellerId: 'miora',
  sellerName: 'Miora Rakoto',
  paymentMethod: 'ORANGE_MONEY',
  issuerName: 'By Valimbavaka Cosmetics',
  issuerNif: '4001234567',
  subtotal: 29000,
  discountAmount: 1500,
  total: 27500,
  lines: [
    { articleId: 'savon', label: 'Savon coco', quantity: 2, unitPrice: 8500, lineTotal: 17000 },
    {
      articleId: 'huile',
      label: 'Huile de ricin',
      quantity: 1,
      unitPrice: 12000,
      lineTotal: 12000,
    },
  ],
};

describe('sales mapping', () => {
  it('résume les articles de la vente avec leur quantité', () => {
    expect(articlesLabel(SALE.lines)).toBe('Savon coco ×2 · Huile de ricin ×1');
  });

  it("garde l'identifiant d'un article dont la fiche est introuvable", () => {
    expect(
      articlesLabel([{ articleId: 'a-1', articleName: null, quantity: 0.5, lineTotal: 1 }]),
    ).toBe('a-1 ×0.5');
  });

  it('construit la ligne du tableau : référence, canal, vendeuse, articles, règlement, promo et total', () => {
    const row = toSaleRow({ ...SALE, discountAmount: 1500, total: 27500 });

    expect(row).toEqual(
      expect.objectContaining({
        number: 'VTE-0042',
        channelLabel: 'Boutique',
        sellerName: 'Miora Rakoto',
        articles: 'Savon coco ×2 · Huile de ricin ×1',
        payment: 'MVOLA',
        discount: 1500,
        total: 27500,
        status: 'VALIDATED',
      }),
    );
    expect(row.time).toMatch(/^\d{2}:\d{2}$/);
  });

  it('garde une vente sans vendeuse ni canal actif sans nom (affichés « Sans vendeuse » et « — »)', () => {
    const row = toSaleRow({ ...SALE, sellerId: null, sellerName: null, salesChannelLabel: null });

    expect(row.sellerName).toBeNull();
    expect(row.channelLabel).toBeNull();
  });

  it.each(['CASH', 'MVOLA', 'ORANGE_MONEY', 'CARD', 'ON_DELIVERY'] as const)(
    'reporte le mode de règlement %s de la vente sur la ligne du tableau et sur la facture (VTE-05)',
    (paymentMethod) => {
      const sale = { ...SALE, paymentMethod };

      expect(toSaleRow(sale).payment).toBe(paymentMethod);
      expect(toInvoicePreview(sale).payment).toBe(paymentMethod);
    },
  );

  it("prépare l'aperçu de facture non émise depuis la vente", () => {
    const invoice = toInvoicePreview(SALE);

    expect(invoice.number).toBeNull();
    expect(invoice.status).toBe('PENDING');
    expect(invoice.saleNumber).toBe('VTE-0042');
    expect(invoice.lines).toEqual([
      { label: 'Savon coco', quantity: 2, total: 17000 },
      { label: 'Huile de ricin', quantity: 1, total: 12000 },
    ]);
    expect(invoice.total).toBe(29000);
  });

  it("montre l'avoir d'une vente contre-passée", () => {
    expect(toInvoicePreview({ ...SALE, status: 'REVERSED' }).status).toBe('CREDITED');
  });

  it('donne le jour local au format yyyy-MM-dd', () => {
    expect(localIsoDate(new Date(2026, 10, 8, 0, 30))).toBe('2026-11-08');
  });

  it('prépare le document de la facture émise : numéro, date, émetteur, lignes figées et totaux (FAC-01)', () => {
    const view = toInvoiceView(INVOICE);

    expect(view.number).toBe('FAC-2026-0042');
    expect(view.issuedAt).toMatch(/^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/);
    expect(view.status).toBe('PAID');
    expect(view).toEqual(
      expect.objectContaining({
        saleNumber: 'VTE-0042',
        channelLabel: 'Boutique',
        sellerName: 'Miora Rakoto',
        payment: 'ORANGE_MONEY',
        issuerName: 'By Valimbavaka Cosmetics',
        issuerNif: '4001234567',
        subtotal: 29000,
        discount: 1500,
        total: 27500,
      }),
    );
    expect(view.lines).toEqual([
      { label: 'Savon coco', quantity: 2, total: 17000 },
      { label: 'Huile de ricin', quantity: 1, total: 12000 },
    ]);
  });

  it('garde une facture sans vendeuse ni NIF telle quelle (affichés « Sans vendeuse » et « [à compléter] »)', () => {
    const view = toInvoiceView({ ...INVOICE, sellerId: null, sellerName: null, issuerNif: null });

    expect(view.sellerName).toBeNull();
    expect(view.issuerNif).toBeNull();
  });

  it.each(['CANCELLED', 'REVERSED'] as const)(
    "montre l'avoir d'une facture dont la vente est %s",
    (status) => {
      expect(toInvoiceView({ ...INVOICE, status }).status).toBe('CREDITED');
    },
  );

  it("donne la date et l'heure locales au format jj/mm/aaaa HH:MM", () => {
    expect(localDateTime(new Date(2026, 10, 8, 9, 5).toISOString())).toBe('08/11/2026 09:05');
  });
});
