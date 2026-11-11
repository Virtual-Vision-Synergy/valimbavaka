import { RevenueSplitRDto } from './revenue-split-models';

/** Répartition du CA du jour telle que l'API la renvoie (1 284 000 Ar vendus, un canal désactivé depuis). Tests. */
export const REVENUE_SPLIT_TEST_VIEW: RevenueSplitRDto = {
  date: '2026-11-11',
  byRange: [
    { rangeId: 'r-1', code: 'ZEZIKA', label: 'Zezika', revenue: 562_000, sharePercent: 43.8 },
    { rangeId: 'r-2', code: 'SOLIKA', label: 'Solika', revenue: 436_000, sharePercent: 33.9 },
    { rangeId: 'r-3', code: 'BOTANIKA', label: 'Botanika', revenue: 286_000, sharePercent: 22.3 },
  ],
  byChannel: [
    { salesChannelId: 'c-1', label: 'Boutique', revenue: 668_000, sharePercent: 52.0 },
    { salesChannelId: 'c-2', label: 'Facebook', revenue: 398_000, sharePercent: 31.0 },
    { salesChannelId: 'c-3', label: null, revenue: 218_000, sharePercent: 17.0 },
  ],
};

/** Journée sans aucune vente validée : les listes sont présentes, tout est à 0. Tests. */
export const REVENUE_SPLIT_EMPTY_TEST_VIEW: RevenueSplitRDto = {
  date: '2026-11-11',
  byRange: [
    { rangeId: 'r-1', code: 'ZEZIKA', label: 'Zezika', revenue: 0, sharePercent: 0 },
    { rangeId: 'r-3', code: 'BOTANIKA', label: 'Botanika', revenue: 0, sharePercent: 0 },
  ],
  byChannel: [{ salesChannelId: 'c-1', label: 'Boutique', revenue: 0, sharePercent: 0 }],
};
