import { PageResponse } from '../../../shared/models/page-response';
import { PromotionRDto, PromotionReferences } from './promotion-models';

/** Données de test des promotions (PRO-04) : référentiels et fiches au format du backend. */
export const PROMOTION_REFS: PromotionReferences = {
  ranges: [{ id: 'range-zezika', code: 'ZEZIKA', label: 'Zezika' }],
  channels: [
    { id: 'channel-boutique', code: 'BOUTIQUE', label: 'Boutique' },
    { id: 'channel-facebook', code: 'FACEBOOK', label: 'Facebook' },
  ],
};

export function buildPromotion(overrides: Partial<PromotionRDto> = {}): PromotionRDto {
  return {
    id: 'promo-1',
    name: 'Fête des mères',
    description: 'Gamme Zezika',
    kind: 'PERCENT',
    value: 20,
    code: null,
    scope: 'RANGES',
    targetIds: ['range-zezika'],
    startsOn: '2026-11-01',
    endsOn: '2026-11-30',
    channelIds: ['channel-facebook'],
    usageLimit: 200,
    usageCount: 52,
    status: 'ACTIVE',
    createdAt: '2026-10-30T08:00:00Z',
    updatedAt: '2026-10-30T08:00:00Z',
    version: 0,
    ...overrides,
  };
}

export function pageOf(
  items: PromotionRDto[],
  totalCount = items.length,
): PageResponse<PromotionRDto> {
  return { items, page: 1, pageSize: 12, totalCount, totalPages: Math.ceil(totalCount / 12) };
}
