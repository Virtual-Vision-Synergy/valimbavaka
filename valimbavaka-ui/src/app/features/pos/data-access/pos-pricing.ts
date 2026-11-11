/**
 * Calcul du ticket au poste (VTE-02), en temps réel et hors connexion. Il reprend la règle du serveur
 * (`Sale.record` / `SaleLine.of`, VTE-01) : total de ligne = prix × quantité arrondi HALF_UP à 2 décimales,
 * sous-total = somme des lignes, net = sous-total − remise. Les montants sont additionnés en centimes entiers
 * (0,10 × 3 = 0,30 et non 0,30000000000000004). Le serveur recalcule tout à l'encaissement et fait foi.
 *
 * Promotion automatique (PRO-02) : mêmes règles que `PromotionPricing` du serveur, sur les mêmes exemples
 * (remise arrondie à l'ariary, bornée à ce dont elle est retirée, une seule promotion, la plus avantageuse).
 */
import { PosPromotionRDto, SaleChannel } from './pos-models';

const CENTS_PER_UNIT = 100;
/** Les quantités ont au plus 3 décimales (`sale_lines.quantity NUMERIC(12,3)`). */
const QUANTITY_SCALE = 1000;

/** Totaux affichés du ticket. */
export interface TicketTotals {
  readonly subtotal: number;
  readonly discount: number;
  readonly total: number;
}

/** Ligne du ticket avant remise : article, prix unitaire, quantité et total de la ligne. */
export interface PromotionLine {
  readonly articleId: string;
  readonly unitPrice: number;
  readonly quantity: number;
  readonly gross: number;
}

/** Remise retenue : remise de chaque ligne (même ordre ; vide sans promotion) et remise du ticket (`AMOUNT`). */
export interface AppliedPromotion {
  readonly promotion: PosPromotionRDto | null;
  readonly lineDiscounts: readonly number[];
  readonly ticketDiscount: number;
}

/** Ligne déjà valorisée : total de la ligne et remise de la ligne. */
export interface LineAmounts {
  readonly gross: number;
  readonly discount: number;
}

function toCents(amount: number): number {
  return Math.round(amount * CENTS_PER_UNIT);
}

function fromCents(cents: number): number {
  return cents / CENTS_PER_UNIT;
}

/** Total d'une ligne comme le serveur : prix × quantité, arrondi HALF_UP au centime. */
export function lineTotal(unitPrice: number, quantity: number): number {
  const product = toCents(unitPrice) * Math.round(quantity * QUANTITY_SCALE);
  return fromCents(Math.floor((product + QUANTITY_SCALE / 2) / QUANTITY_SCALE));
}

const NO_PROMOTION: AppliedPromotion = { promotion: null, lineDiscounts: [], ticketDiscount: 0 };
/** Un pourcentage s'exprime au centième : montant en centimes × taux en centièmes ÷ (100 × 100 × 100). */
const PERCENT_DIVISOR = 1_000_000;

/** Division entière arrondie HALF_UP (montants positifs). */
function divideHalfUp(numerator: number, divisor: number): number {
  return Math.floor((numerator + divisor / 2) / divisor);
}

/** Remise de la ligne d'une promotion de ligne (`PERCENT`, `BUY_X`), en ariary, bornée au total de la ligne. */
function lineOff(promotion: PosPromotionRDto, line: PromotionLine): number {
  if (promotion.kind === 'PERCENT') {
    const rate = Math.round(promotion.value * CENTS_PER_UNIT);
    return Math.min(line.gross, divideHalfUp(toCents(line.gross) * rate, PERCENT_DIVISOR));
  }
  const group = Math.round(promotion.value * QUANTITY_SCALE);
  const free = Math.floor(Math.round(line.quantity * QUANTITY_SCALE) / group);
  return Math.min(line.gross, divideHalfUp(free * toCents(line.unitPrice), CENTS_PER_UNIT));
}

/** Remise d'une promotion sur les lignes qu'elle atteint ; 0 pour les autres. */
function applyPromotion(
  promotion: PosPromotionRDto,
  lines: readonly PromotionLine[],
): AppliedPromotion {
  const reached = lines.map((line) => promotion.articleIds.includes(line.articleId));
  if (promotion.kind === 'AMOUNT') {
    const eligible = lines.reduce(
      (sum, line, i) => sum + (reached[i] ? toCents(line.gross) : 0),
      0,
    );
    const amount = Math.min(Math.round(promotion.value), fromCents(eligible));
    return { promotion, lineDiscounts: lines.map(() => 0), ticketDiscount: amount };
  }
  const lineDiscounts = lines.map((line, i) => (reached[i] ? lineOff(promotion, line) : 0));
  return { promotion, lineDiscounts, ticketDiscount: 0 };
}

function totalOff(applied: AppliedPromotion): number {
  return applied.lineDiscounts.reduce(
    (sum, amount) => sum + toCents(amount),
    toCents(applied.ticketDiscount),
  );
}

/**
 * Promotion automatique du canal qui donne la plus grande remise sur le ticket (la première en cas d'égalité,
 * dans l'ordre du serveur : fin la plus proche) ; pas de cumul, aucune si aucune n'a d'effet.
 */
export function bestPromotion(
  promotions: readonly PosPromotionRDto[],
  channel: SaleChannel,
  lines: readonly PromotionLine[],
): AppliedPromotion {
  let best = NO_PROMOTION;
  let bestOff = 0;
  for (const promotion of promotions.filter((item) => item.channels.includes(channel))) {
    const applied = applyPromotion(promotion, lines);
    if (totalOff(applied) > bestOff) {
      best = applied;
      bestOff = totalOff(applied);
    }
  }
  return best;
}

/** Sous-total, remise et net à payer du ticket (0 pour un ticket vide) ; `ticketDiscount` : remise hors ligne. */
export function ticketTotals(lines: readonly LineAmounts[], ticketDiscount = 0): TicketTotals {
  const subtotal = lines.reduce((sum, line) => sum + toCents(line.gross), 0);
  const discount =
    lines.reduce((sum, line) => sum + toCents(line.discount), 0) + toCents(ticketDiscount);
  return {
    subtotal: fromCents(subtotal),
    discount: fromCents(discount),
    total: fromCents(subtotal - discount),
  };
}

/** Rendu monnaie (négatif : montant qui manque). */
export function changeDue(received: number, total: number): number {
  return fromCents(toCents(received) - toCents(total));
}
