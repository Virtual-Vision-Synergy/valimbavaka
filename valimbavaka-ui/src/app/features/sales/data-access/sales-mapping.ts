import { InvoiceRDto, SaleRowRDto, SaleSummaryLineRDto, SaleSummaryRDto } from './sales-models';

/** Raison sociale imprimée sur les factures (le NIF reste à compléter, FAC-01). */
export const ISSUER_NAME = 'By Valimbavaka Cosmetics';

/** « Savon coco ×2 · Huile de ricin ×1 » ; un article sans nom (fiche introuvable) garde son identifiant. */
export function articlesLabel(lines: readonly SaleSummaryLineRDto[]): string {
  return lines.map((line) => `${line.articleName ?? line.articleId} ×${line.quantity}`).join(' · ');
}

/** Heure locale `HH:MM` d'un instant ISO. */
export function localTime(instant: string): string {
  return new Date(instant).toTimeString().slice(0, 5);
}

/** Ligne du tableau des ventes du jour (VTE-07). */
export function toSaleRow(sale: SaleSummaryRDto): SaleRowRDto {
  return {
    id: sale.id,
    number: sale.number,
    time: localTime(sale.createdAt),
    channelLabel: sale.salesChannelLabel,
    sellerName: sale.sellerName,
    articles: articlesLabel(sale.lines),
    payment: sale.paymentMethod,
    discount: sale.discountAmount,
    total: sale.total,
    status: sale.status,
  };
}

/**
 * Aperçu de la facture d'une vente, construit depuis la vente elle-même tant que les factures ne sont pas émises par
 * le serveur (FAC-01) : sans numéro ni date d'émission, statut `PENDING` ; une vente contre-passée a son avoir.
 */
export function toInvoicePreview(sale: SaleSummaryRDto): InvoiceRDto {
  return {
    number: null,
    issuedAt: null,
    status: sale.status === 'REVERSED' ? 'CREDITED' : 'PENDING',
    saleNumber: sale.number,
    channelLabel: sale.salesChannelLabel,
    sellerName: sale.sellerName,
    payment: sale.paymentMethod,
    issuerName: ISSUER_NAME,
    issuerNif: null,
    lines: sale.lines.map((line) => ({
      label: line.articleName ?? line.articleId,
      quantity: line.quantity,
      total: line.lineTotal,
    })),
    subtotal: sale.subtotal,
    discountLabel: null,
    discount: sale.discountAmount,
    total: sale.total,
  };
}

/** Jour local `yyyy-MM-dd` d'une date (le jour de la boutique, pas celui d'UTC). */
export function localIsoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}
