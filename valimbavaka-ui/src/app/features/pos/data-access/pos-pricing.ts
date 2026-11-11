/**
 * Calcul du ticket au poste (VTE-02), en temps réel et hors connexion. Il reprend la règle du serveur
 * (`Sale.record` / `SaleLine.of`, VTE-01) : total de ligne = prix × quantité arrondi HALF_UP à 2 décimales,
 * sous-total = somme des lignes, net = sous-total − remise. Les montants sont additionnés en centimes entiers
 * (0,10 × 3 = 0,30 et non 0,30000000000000004). Le serveur recalcule tout à l'encaissement et fait foi.
 */

const CENTS_PER_UNIT = 100;
/** Les quantités ont au plus 3 décimales (`sale_lines.quantity NUMERIC(12,3)`). */
const QUANTITY_SCALE = 1000;

/** Totaux affichés du ticket. */
export interface TicketTotals {
  readonly subtotal: number;
  readonly discount: number;
  readonly total: number;
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

/**
 * Remise d'une ligne (taux en %), arrondie à l'ariary comme sur la maquette ; bornée au total de la ligne
 * pour que le net ne soit jamais négatif (contrainte `0 ≤ remise ≤ sous-total` de `sales`).
 */
export function lineDiscount(gross: number, rate: number): number {
  if (rate <= 0) {
    return 0;
  }
  return Math.min(gross, Math.round((gross * rate) / 100));
}

/** Sous-total, remise et net à payer du ticket (0 pour un ticket vide). */
export function ticketTotals(lines: readonly LineAmounts[]): TicketTotals {
  const subtotal = lines.reduce((sum, line) => sum + toCents(line.gross), 0);
  const discount = lines.reduce((sum, line) => sum + toCents(line.discount), 0);
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
