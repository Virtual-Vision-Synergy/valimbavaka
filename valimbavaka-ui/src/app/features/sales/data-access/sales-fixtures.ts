import { demoArticle } from '../../../shared/data-access/demo-catalog';
import { PaymentMethod, SALE_CHANNELS, SaleChannel } from '../../../shared/models/commerce';
import { InvoiceRDto, InvoiceStatus, SaleRDto, SalesDayRDto } from './sales-models';

/** [numéro, heure, canal, vendeuse, règlement, statut, lignes [référence, quantité], remise]. */
type SaleRow = readonly [
  number,
  string,
  SaleChannel,
  string,
  PaymentMethod,
  InvoiceStatus,
  readonly (readonly [string, number])[],
  number,
];

/** Les dix ventes visibles sur la maquette VTE-07 (montants identiques). */
const MOCKUP_ROWS: readonly SaleRow[] = [
  [
    2452,
    '10:52',
    'SHOP',
    'Miora A.',
    'CASH',
    'PAID',
    [
      ['ART-0031', 3],
      ['ART-0154', 1],
      ['ART-0190', 1],
      ['ART-0087', 1],
    ],
    5700,
  ],
  [
    2451,
    '10:48',
    'SHOP',
    'Miora A.',
    'CASH',
    'PAID',
    [
      ['ART-0031', 2],
      ['ART-0133', 1],
      ['ART-0176', 1],
      ['ART-0201', 1],
    ],
    2550,
  ],
  [
    2450,
    '10:31',
    'FACEBOOK',
    'Lova R.',
    'MVOLA',
    'SENT',
    [
      ['ART-0154', 1],
      ['ART-0162', 1],
    ],
    0,
  ],
  [
    2449,
    '10:12',
    'PHONE',
    'Lova R.',
    'ON_DELIVERY',
    'PENDING',
    [
      ['ART-0154', 1],
      ['ART-0133', 1],
      ['ART-0112', 1],
    ],
    0,
  ],
  [
    2448,
    '09:58',
    'FACEBOOK',
    'Lova R.',
    'MVOLA',
    'PAID',
    [
      ['ART-0162', 1],
      ['ART-0154', 1],
    ],
    0,
  ],
  [
    2447,
    '09:41',
    'SHOP',
    'Miora A.',
    'CARD',
    'PAID',
    [
      ['ART-0240', 1],
      ['ART-0162', 1],
      ['ART-0118', 1],
      ['ART-0190', 1],
    ],
    0,
  ],
  [2446, '09:30', 'SHOP', 'Miora A.', 'CASH', 'CREDITED', [['ART-0031', 1]], 0],
  [
    2445,
    '09:22',
    'FACEBOOK',
    'Lova R.',
    'ORANGE_MONEY',
    'PAID',
    [
      ['ART-0118', 1],
      ['ART-0176', 1],
    ],
    0,
  ],
  [
    2444,
    '09:05',
    'SHOP',
    'Miora A.',
    'CASH',
    'PAID',
    [
      ['ART-0112', 1],
      ['ART-0201', 1],
    ],
    0,
  ],
  [
    2443,
    '08:48',
    'PHONE',
    'Lova R.',
    'ON_DELIVERY',
    'PENDING',
    [
      ['ART-0087', 1],
      ['ART-0176', 1],
      ['ART-0118', 1],
    ],
    0,
  ],
];

/** Paniers types pour compléter la journée de démonstration (47 ventes). */
const BASKETS: readonly (readonly (readonly [string, number])[])[] = [
  [
    ['ART-0031', 1],
    ['ART-0201', 1],
  ],
  [['ART-0133', 1]],
  [['ART-0045', 2]],
  [
    ['ART-0190', 1],
    ['ART-0118', 1],
  ],
  [
    ['ART-0176', 1],
    ['ART-0031', 1],
  ],
  [['ART-0162', 1]],
];
const PAYMENTS: readonly PaymentMethod[] = ['CASH', 'MVOLA', 'CASH', 'CARD', 'ORANGE_MONEY'];

function generatedRow(index: number): SaleRow {
  const number = 2442 - index;
  const minutes = 8 * 60 + 40 - index * 2;
  const time = `${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`;
  const channel = SALE_CHANNELS[index % 5 === 4 ? 2 : index % 3 === 2 ? 1 : 0];
  const seller = channel === 'SHOP' ? 'Miora A.' : 'Lova R.';
  const payment = channel === 'PHONE' ? 'ON_DELIVERY' : PAYMENTS[index % PAYMENTS.length];
  return [number, time, channel, seller, payment, 'PAID', BASKETS[index % BASKETS.length], 0];
}

const ROWS: readonly SaleRow[] = [
  ...MOCKUP_ROWS,
  ...Array.from({ length: 37 }, (_, index) => generatedRow(index)),
];

function invoiceNumber(saleNumber: number): string {
  return `FAC-2026-${String(saleNumber - 969).padStart(5, '0')}`;
}

function gross(row: SaleRow): number {
  return row[6].reduce(
    (sum, [reference, quantity]) => sum + demoArticle(reference).price * quantity,
    0,
  );
}

function toSale(row: SaleRow): SaleRDto {
  const [number, time, channel, sellerName, payment, status, , discount] = row;
  return {
    id: `sale-${number}`,
    number: `VTE-${number}`,
    time,
    channel,
    sellerName,
    payment,
    amount: gross(row) - discount,
    status,
    invoiceNumber: invoiceNumber(number),
  };
}

const SALES: readonly SaleRDto[] = ROWS.map(toSale);

export const SALES_DAY_FIXTURE: SalesDayRDto = {
  date: '2026-10-21',
  total: SALES.reduce((sum, sale) => sum + sale.amount, 0),
  count: SALES.length,
  byChannel: SALE_CHANNELS.map((channel) => {
    const sales = SALES.filter((sale) => sale.channel === channel);
    return {
      channel,
      amount: sales.reduce((sum, sale) => sum + sale.amount, 0),
      count: sales.length,
    };
  }),
  sales: SALES,
};

/** Facture de démonstration d'une vente (lignes valorisées depuis le catalogue). */
export function invoiceFixture(saleNumber: string): InvoiceRDto | null {
  const row = ROWS.find(([number]) => `VTE-${number}` === saleNumber);
  if (!row) {
    return null;
  }
  const sale = toSale(row);
  const discount = row[7];
  return {
    number: sale.invoiceNumber,
    issuedAt: `21/10/2026 · ${sale.time}`,
    status: sale.status,
    saleNumber: sale.number,
    channel: sale.channel,
    sellerName: sale.sellerName,
    payment: sale.payment,
    issuerName: 'By Valimbavaka Cosmetics',
    issuerNif: null,
    lines: row[6].map(([reference, quantity]) => {
      const article = demoArticle(reference);
      return { label: article.name, quantity, total: article.price * quantity };
    }),
    subtotal: gross(row),
    discountLabel: discount ? 'Octobre rose' : null,
    discount,
    total: sale.amount,
  };
}
