import { ProductRange } from '../../../shared/models/tone';
import { DashboardRDto } from './dashboard-models';

/** Référence, désignation, gamme, emplacement, quantité, seuil minimum. */
type StockAlertRow = readonly [string, string, ProductRange, string, number, number];

const STOCK_ALERTS: readonly StockAlertRow[] = [
  ['ART-0112', 'Huile de coco vierge 250 ml', 'BOTANIKA', 'Boutique', 0, 12],
  ['ART-0087', 'Gommage café 200 g', 'ZEZIKA', 'Boutique', 3, 10],
  ['ART-0154', 'Beurre de karité 150 g', 'SOLIKA', 'Entrepôt', 6, 20],
  ['ART-0031', 'Savon noir 100 g', 'BOTANIKA', 'Livraison', 2, 8],
  ['ART-0201', 'Baume lèvres miel 15 g', 'ZEZIKA', 'Salon', 1, 6],
];

/**
 * Données de démonstration du cockpit (articles issus du jeu de test v1) : remplacées par
 * GET /api/v1/dashboard dès que le module backend existe. Seul DashboardService les référence.
 */
export const DASHBOARD_FIXTURE: DashboardRDto = {
  businessDate: '2026-10-21',
  openedAt: '07:42',
  openedBy: 'Hanta R.',
  updatedAt: '21:46',
  monthlyTarget: 32_000_000,
  monthlyRevenue: 21_480_000,
  steps: [
    { code: 'OPENING', state: 'done', params: { time: '07:42' } },
    { code: 'SALES', state: 'current', params: { tickets: 47 } },
    { code: 'DELIVERIES', state: 'current', params: { done: 9, total: 14 } },
    { code: 'CLOSING', state: 'upcoming', params: { time: '19:00' } },
  ],
  revenue: {
    amount: 1_284_000,
    previousAmount: 1_142_500,
    changePercent: 12.4,
    dailyTarget: 1_650_000,
  },
  sales: {
    tickets: 47,
    delta: 5,
    averageBasket: 27_320,
    byChannel: { SHOP: 26, FACEBOOK: 14, PHONE: 7 },
  },
  deliveries: { done: 9, total: 14, inProgress: 3, failed: 2, activeRounds: 2, collected: 312_000 },
  stockGap: { units: -4, articles: 3 },
  revenueLast7Days: [
    { date: '2026-10-15', amount: 980_000 },
    { date: '2026-10-16', amount: 1_120_000 },
    { date: '2026-10-17', amount: 1_460_000 },
    { date: '2026-10-18', amount: 1_710_000 },
    { date: '2026-10-19', amount: 640_000 },
    { date: '2026-10-20', amount: 1_050_000 },
    { date: '2026-10-21', amount: 1_284_000 },
  ],
  revenueLast4Weeks: [
    { date: '2026-09-28', amount: 6_420_000 },
    { date: '2026-10-05', amount: 7_180_000 },
    { date: '2026-10-12', amount: 7_640_000 },
    { date: '2026-10-19', amount: 2_974_000 },
  ],
  revenueByRange: [
    { range: 'BOTANIKA', amount: 562_000 },
    { range: 'ZEZIKA', amount: 436_000 },
    { range: 'SOLIKA', amount: 286_000 },
  ],
  revenueByChannel: [
    { channel: 'SHOP', amount: 668_000 },
    { channel: 'FACEBOOK', amount: 398_000 },
    { channel: 'PHONE', amount: 218_000 },
  ],
  stockAlerts: STOCK_ALERTS.map(([reference, name, range, location, quantity, threshold]) => ({
    articleId: reference.toLowerCase(),
    reference,
    name,
    range,
    location,
    quantity,
    threshold,
  })),
  activity: [
    {
      id: 'a1',
      kind: 'SALE',
      actor: 'Miora A.',
      params: { reference: 'VTE-2451', amount: 54_000, place: 'Boutique' },
      minutesAgo: 2,
    },
    {
      id: 'a2',
      kind: 'DELIVERY',
      actor: 'Tojo N.',
      params: { reference: 'LIV-0318', place: 'Analakely', amount: 18_000 },
      minutesAgo: 6,
    },
    {
      id: 'a3',
      kind: 'DELIVERY_FAILED',
      actor: 'Tojo N.',
      params: { reference: 'LIV-0315', reason: 'client absent' },
      minutesAgo: 14,
    },
    {
      id: 'a4',
      kind: 'SERVICE',
      actor: 'Fara R.',
      params: { service: 'soin visage', place: 'Salon' },
      minutesAgo: 21,
    },
    { id: 'a5', kind: 'SHOOTING', actor: 'Miora A.', params: { count: 2 }, minutesAgo: 38 },
    {
      id: 'a6',
      kind: 'TRANSFER',
      actor: 'Hanta R.',
      params: { reference: 'TRF-0092', from: 'Entrepôt', to: 'Boutique' },
      minutesAgo: 844,
    },
  ],
  topArticles: [
    {
      reference: 'ART-0045',
      name: 'Crème hydratante aloe 50 ml',
      range: 'BOTANIKA',
      quantity: 12,
      amount: 216_000,
    },
    {
      reference: 'ART-0087',
      name: 'Gommage café 200 g',
      range: 'ZEZIKA',
      quantity: 9,
      amount: 162_000,
    },
    {
      reference: 'ART-0154',
      name: 'Beurre de karité 150 g',
      range: 'SOLIKA',
      quantity: 8,
      amount: 136_000,
    },
    {
      reference: 'ART-0112',
      name: 'Huile de coco vierge 250 ml',
      range: 'BOTANIKA',
      quantity: 7,
      amount: 122_500,
    },
    {
      reference: 'ART-0201',
      name: 'Baume lèvres miel 15 g',
      range: 'ZEZIKA',
      quantity: 11,
      amount: 77_000,
    },
  ],
  sellers: [
    { name: 'Miora Andrianina', tickets: 21, amount: 598_000, target: 650_000 },
    { name: 'Fara Rakoto', tickets: 15, amount: 402_000, target: 550_000 },
    { name: 'Lova Razafy', tickets: 11, amount: 284_000, target: 450_000 },
  ],
};
