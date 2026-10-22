import { isoDay } from '../../../shared/data-access/demo-dates';
import { SaleChannel } from '../../../shared/models/commerce';
import { CommissionLineRDto, CommissionStatementRDto, CommissionStatus } from './commission-models';

/** [vente, jours avant aujourd'hui, canal, base, taux %, statut]. */
type LineRow = readonly [string, number, SaleChannel, number, number, CommissionStatus];

const CURRENT_ROWS: readonly LineRow[] = [
  ['VTE-2452', 0, 'SHOP', 58300, 3, 'CALCULATED'],
  ['VTE-2451', 0, 'SHOP', 50950, 3, 'CALCULATED'],
  ['VTE-2447', 0, 'SHOP', 72000, 3, 'CALCULATED'],
  ['VTE-2431', 1, 'FACEBOOK', 64000, 5, 'VALIDATED'],
  ['VTE-2428', 1, 'PHONE', 45500, 4, 'VALIDATED'],
  ['VTE-2410', 2, 'SHOP', 38000, 3, 'VALIDATED'],
  ['VTE-2395', 3, 'FACEBOOK', 92000, 5, 'VALIDATED'],
  ['VTE-2203', 22, 'SHOP', 41000, 3, 'PAID'],
];

function toLine([sale, ago, channel, base, ratePercent, status]: LineRow): CommissionLineRDto {
  return {
    sale,
    date: isoDay(-ago),
    channel,
    base,
    ratePercent,
    amount: Math.round((base * ratePercent) / 100),
    status,
  };
}

function monthKey(offset: number): string {
  const day = new Date();
  day.setDate(1);
  day.setMonth(day.getMonth() + offset);
  return `${day.getFullYear()}-${String(day.getMonth() + 1).padStart(2, '0')}`;
}

/** Mois proposés dans le sélecteur : le mois en cours puis le précédent. */
export const COMMISSION_MONTHS: readonly string[] = [monthKey(0), monthKey(-1)];

const CURRENT: CommissionStatementRDto = {
  month: COMMISSION_MONTHS[0],
  seller: 'Miora Andriamasy',
  sales: 3_240_000,
  salesCount: 118,
  goal: 4_500_000,
  validated: 84200,
  toValidate: 44440,
  workingDaysLeft: 8,
  channels: [
    { channel: 'SHOP', ratePercent: 3, amount: 52800, base: 1_760_000 },
    { channel: 'FACEBOOK', ratePercent: 5, amount: 55000, base: 1_100_000 },
    { channel: 'PHONE', ratePercent: 4, amount: 15200, base: 380_000 },
    { channel: 'SALON', ratePercent: null, amount: 5640, base: 6 },
  ],
  lines: CURRENT_ROWS.map(toLine),
};

const PREVIOUS: CommissionStatementRDto = {
  ...CURRENT,
  month: COMMISSION_MONTHS[1],
  sales: 4_620_000,
  salesCount: 163,
  validated: 176300,
  toValidate: 0,
  workingDaysLeft: 0,
  channels: [
    { channel: 'SHOP', ratePercent: 3, amount: 79200, base: 2_640_000 },
    { channel: 'FACEBOOK', ratePercent: 5, amount: 71500, base: 1_430_000 },
    { channel: 'PHONE', ratePercent: 4, amount: 17200, base: 430_000 },
    { channel: 'SALON', ratePercent: null, amount: 8400, base: 9 },
  ],
  lines: CURRENT_ROWS.map((row, index) =>
    toLine([row[0], 30 + index, row[2], row[3], row[4], 'PAID']),
  ),
};

export function statementFixture(month: string): CommissionStatementRDto {
  return month === COMMISSION_MONTHS[1] ? PREVIOUS : CURRENT;
}
