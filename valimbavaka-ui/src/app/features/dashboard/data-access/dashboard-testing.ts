import { DashboardRDto } from './dashboard-models';

/** Cockpit tel que l'API le renvoie un jour d'activité (journée ouverte, un écart de stock). Réservé aux tests. */
export const DASHBOARD_TEST_VIEW: DashboardRDto = {
  businessDate: '2026-11-11',
  status: 'OPEN',
  generatedAt: '2026-11-11T10:05:00Z',
  revenue: { amount: 1_284_000, previousAmount: 1_142_500, changePercent: 12.4 },
  sales: {
    count: 47,
    previousCount: 42,
    averageBasket: 27_319.15,
    channels: [
      {
        salesChannelId: '7f1c2a9e-0000-4000-8000-000000000001',
        salesChannelLabel: 'Boutique',
        revenue: 700_000,
        salesCount: 26,
        revenueSharePercent: 54.5,
      },
      {
        salesChannelId: '7f1c2a9e-0000-4000-8000-000000000002',
        salesChannelLabel: 'Facebook',
        revenue: 584_000,
        salesCount: 21,
        revenueSharePercent: 45.5,
      },
    ],
  },
  deliveries: { total: 14, delivered: 9, onRound: 2, toPrepare: 1, failed: 2, cancelled: 1 },
  stockGap: { units: -4, articles: 3 },
};
