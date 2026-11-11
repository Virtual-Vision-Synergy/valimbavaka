import {
  InvoiceRDto,
  InvoiceView,
  SaleRowRDto,
  SaleSummaryLineRDto,
  SaleSummaryRDto,
} from './sales-models';

/** Raison sociale de l'aperçu d'une vente sans facture (les factures émises portent celle du serveur). */
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

/** Date et heure locales `jj/mm/aaaa HH:MM` d'un instant ISO (l'heure de la boutique, pas celle d'UTC). */
export function localDateTime(instant: string): string {
  const date = new Date(instant);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${day}/${month}/${date.getFullYear()} ${localTime(instant)}`;
}

/**
 * Facture émise par le serveur (FAC-01), prête pour le document papier : numéro et date d'émission, lignes figées.
 * Une facture encaissée est « Payée » (FAC-05 gérera « Envoyée ») ; une vente annulée ou contre-passée a son avoir.
 */
export function toInvoiceView(invoice: InvoiceRDto): InvoiceView {
  return {
    number: invoice.number,
    issuedAt: localDateTime(invoice.issuedAt),
    status: invoice.status === 'VALIDATED' ? 'PAID' : 'CREDITED',
    saleNumber: invoice.saleNumber,
    channelLabel: invoice.salesChannelLabel,
    sellerName: invoice.sellerName,
    payment: invoice.paymentMethod,
    issuerName: invoice.issuerName,
    issuerNif: invoice.issuerNif,
    lines: invoice.lines.map((line) => ({
      label: line.label,
      quantity: line.quantity,
      total: line.lineTotal,
    })),
    subtotal: invoice.subtotal,
    discountLabel: null,
    discount: invoice.discountAmount,
    total: invoice.total,
  };
}

/**
 * Aperçu de la facture d'une vente qui n'a pas de facture émise (vente antérieure à FAC-01), construit depuis la vente
 * elle-même : sans numéro ni date d'émission, statut `PENDING` ; une vente contre-passée a son avoir.
 */
export function toInvoicePreview(sale: SaleSummaryRDto): InvoiceView {
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
