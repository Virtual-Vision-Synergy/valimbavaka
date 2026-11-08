import { isoDay } from '../../../shared/data-access/demo-dates';
import { PromotionCardView, PromotionStatus } from './promotion-models';

/** Statut déduit des dates (le serveur fait foi, ceci reflète sa règle pour l'aperçu). */
export function statusOf(
  promotion: Pick<PromotionCardView, 'startsOn' | 'endsOn'>,
): PromotionStatus {
  const today = isoDay(0);
  if (promotion.startsOn > today) {
    return 'SCHEDULED';
  }
  return promotion.endsOn < today ? 'ENDED' : 'ACTIVE';
}
