import { Tone } from '../../../shared/models/tone';
import { formatAriary } from '../../../shared/pipes/ariary-pipe';
import {
  PromotionKind,
  PromotionRDto,
  PromotionReferences,
  PromotionStatus,
} from './promotion-models';

/** Couleur du badge de statut, commune à la carte et à la fiche. */
export const PROMOTION_STATUS_TONES: Readonly<Record<PromotionStatus, Tone>> = {
  ACTIVE: 'success',
  SCHEDULED: 'info',
  ENDED: 'muted',
};

/** Libellé court de la remise (« −15 % », « −5 000 Ar », « 3 = 2 »), commun à la carte et à la fiche. */
export function discountLabel(kind: PromotionKind, value: number): string {
  switch (kind) {
    case 'PERCENT':
      return `−${value} %`;
    case 'AMOUNT':
      return value === 0 ? '0 Ar' : `−${formatAriary(value)}`;
    case 'BUY_X':
      return `${value} = ${value - 1}`;
  }
}

/** Libellés des canaux de la promotion, dans l'ordre du référentiel ; un canal inconnu n'est pas affiché. */
export function channelLabels(promotion: PromotionRDto, refs: PromotionReferences): string[] {
  return refs.channels
    .filter((channel) => promotion.channelIds.includes(channel.id))
    .map((channel) => channel.label);
}

/** Libellés des gammes visées (périmètre `RANGES`) ; vide pour les autres périmètres. */
export function rangeLabels(promotion: PromotionRDto, refs: PromotionReferences): string[] {
  if (promotion.scope !== 'RANGES') {
    return [];
  }
  return refs.ranges
    .filter((range) => promotion.targetIds.includes(range.id))
    .map((range) => range.label);
}
