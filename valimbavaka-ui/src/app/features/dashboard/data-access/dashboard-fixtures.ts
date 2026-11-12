import { DashboardDemoRDto } from './dashboard-models';

/**
 * Données de démonstration du cockpit (articles issus du jeu de test v1) : les 4 indicateurs viennent de
 * GET /api/v1/dashboard ; chaque zone restante sort d'ici quand son API existe. Seul DashboardService les référence.
 */
export const DASHBOARD_DEMO_FIXTURE: DashboardDemoRDto = {
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
