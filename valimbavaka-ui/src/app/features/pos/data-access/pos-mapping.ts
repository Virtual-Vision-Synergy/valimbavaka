import { CurrentUserRDto } from '../../../core/auth/auth-models';
import { ProductRange } from '../../../shared/models/tone';
import { ProductRangeRDto } from '../../referential/data-access/referential-models';
import { CartLine } from './pos-cart';
import {
  PaymentMethod,
  PosArticleRDto,
  PosContextRDto,
  PosPromotionRDto,
  SaleChannel,
  SaleFormRDto,
  SaleWDto,
} from './pos-models';

/** Code du canal du serveur → canal du poste (`STORE` s'affiche « Boutique », comme pour les stocks). */
const POS_CHANNEL_BY_CODE: Readonly<Record<string, SaleChannel>> = {
  STORE: 'SHOP',
  FACEBOOK: 'FACEBOOK',
  PHONE: 'PHONE',
};

const PRODUCT_RANGES: readonly string[] = ['BOTANIKA', 'ZEZIKA', 'SOLIKA'] satisfies ProductRange[];

const STORE_CODE = 'STORE';

/**
 * Prépare le poste depuis le formulaire de vente du serveur : le catalogue affiche le stock de la boutique
 * (les ventes Facebook et téléphone en sortent aussi), la vendeuse connectée est proposée d'office.
 */
export function toPosContext(
  form: SaleFormRDto,
  ranges: readonly ProductRangeRDto[],
  user: CurrentUserRDto | null,
  now: Date,
): PosContextRDto {
  const store = form.channels.find((channel) => channel.code === STORE_CODE);
  const sellers = form.sellers.map((seller) => ({
    id: seller.id,
    name: `${seller.firstName} ${seller.lastName}`,
  }));
  return {
    location: store?.label ?? '',
    cashierName: user ? `${user.firstName} ${user.lastName}` : '',
    sellers,
    defaultSellerId: sellers.some((seller) => seller.id === user?.id) ? (user?.id ?? null) : null,
    channelIds: toChannelIds(form),
    ticketNumber: form.nextNumber,
    openedAt: now.toTimeString().slice(0, 5),
    heldTickets: 0,
    articles: toPosArticles(form, ranges, store?.stockLocationId),
    promotions: toPosPromotions(form),
    openTicket: [],
    openTicketReceived: 0,
  };
}

function toChannelIds(form: SaleFormRDto): Partial<Record<SaleChannel, string>> {
  const channelIds: Partial<Record<SaleChannel, string>> = {};
  for (const channel of form.channels) {
    const posChannel = POS_CHANNEL_BY_CODE[channel.code];
    if (posChannel) {
      channelIds[posChannel] = channel.id;
    }
  }
  return channelIds;
}

/** Promotions automatiques du serveur : les identifiants de canal deviennent les canaux du poste. */
function toPosPromotions(form: SaleFormRDto): PosPromotionRDto[] {
  const posChannels = new Map(
    form.channels.map((channel) => [channel.id, POS_CHANNEL_BY_CODE[channel.code]]),
  );
  return form.promotions.map((promotion) => ({
    id: promotion.id,
    name: promotion.name,
    kind: promotion.kind,
    value: promotion.value,
    articleIds: promotion.articleIds,
    channels: promotion.channelIds.flatMap((id) => {
      const channel = posChannels.get(id);
      return channel ? [channel] : [];
    }),
  }));
}

function toPosArticles(
  form: SaleFormRDto,
  ranges: readonly ProductRangeRDto[],
  locationId: string | undefined,
): PosArticleRDto[] {
  const rangeCodes = new Map(ranges.map((range) => [range.id, range.code]));
  return form.articles.flatMap((article) => {
    const range = article.rangeId ? rangeCodes.get(article.rangeId) : undefined;
    if (!range || !PRODUCT_RANGES.includes(range)) {
      return [];
    }
    return [
      {
        id: article.id,
        reference: article.code,
        barcode: article.barcode ?? '',
        name: article.name,
        price: article.price,
        range: range as ProductRange,
        stock: locationId ? (article.stock[locationId] ?? 0) : 0,
        favorite: false,
        box: false,
      },
    ];
  });
}

/** Corps de `POST /api/v1/sales` : canal, vendeuse, règlement et lignes, sans aucun prix. */
export function toSaleWDto(
  salesChannelId: string,
  sellerId: string,
  paymentMethod: PaymentMethod,
  lines: readonly CartLine[],
): SaleWDto {
  return {
    salesChannelId,
    sellerId,
    paymentMethod,
    lines: lines.map((line) => ({ articleId: line.article.id, quantity: line.quantity })),
  };
}
