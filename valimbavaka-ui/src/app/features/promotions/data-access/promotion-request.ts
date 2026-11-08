import { SaleChannel } from '../../../shared/models/commerce';
import {
  PromotionCardView,
  PromotionDraft,
  PromotionRDto,
  PromotionReferences,
  PromotionWDto,
} from './promotion-models';

/** Code du canal dans le référentiel `sales_channels` (SYS-05) pour chaque canal de l'interface. */
export const CHANNEL_CODES: Readonly<Record<SaleChannel, string>> = {
  SHOP: 'BOUTIQUE',
  FACEBOOK: 'FACEBOOK',
  PHONE: 'TELEPHONE',
};

/** Traduit la saisie du formulaire en `PromotionWDto` (codes de gamme et de canal → identifiants). */
export function toPromotionWDto(draft: PromotionDraft, refs: PromotionReferences): PromotionWDto {
  const range = refs.ranges.find((item) => item.code === draft.scope);
  const channelCodes = draft.channels.map((channel) => CHANNEL_CODES[channel]);
  return {
    name: draft.name,
    description: null,
    kind: draft.kind,
    value: draft.value,
    code: draft.code,
    scope: draft.scope === 'ALL' ? 'ALL' : 'RANGES',
    targetIds: range ? [range.id] : [],
    startsOn: draft.startsOn,
    endsOn: draft.endsOn,
    channelIds: refs.channels
      .filter((item) => channelCodes.includes(item.code))
      .map((item) => item.id),
    usageLimit: draft.limit,
  };
}

/** Carte affichée juste après la création (la remise accordée viendra de PRO-03). */
export function toCardView(promotion: PromotionRDto): PromotionCardView {
  return {
    id: promotion.id,
    name: promotion.name,
    description: promotion.description ?? '',
    kind: promotion.kind,
    value: promotion.value,
    code: promotion.code,
    startsOn: promotion.startsOn,
    endsOn: promotion.endsOn,
    uses: promotion.usageCount,
    limit: promotion.usageLimit,
    discountGranted: 0,
  };
}
