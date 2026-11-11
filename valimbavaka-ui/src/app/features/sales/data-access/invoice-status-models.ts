/** Statut serveur d'une facture (`OperationStatus`) : payée, annulée (avant les avoirs) ou contre-passée. */
export type InvoiceStatusCode = 'VALIDATED' | 'CANCELLED' | 'REVERSED';

/** Factures d'un statut (FAC-05, miroir de `InvoiceStatusTotalRDto`). */
export interface InvoiceStatusTotalRDto {
  readonly status: InvoiceStatusCode;
  readonly invoiceCount: number;
  readonly amount: number;
}

/** Totaux consolidés par statut sur les jours ouvrés `from` à `to` inclus (miroir de `InvoiceStatusTotalsRDto`). */
export interface InvoiceStatusTotalsRDto {
  readonly from: string;
  readonly to: string;
  readonly invoiceCount: number;
  readonly amount: number;
  readonly statuses: readonly InvoiceStatusTotalRDto[];
}

/** Période de `GET /api/v1/invoices/status-totals` : absente, le serveur prend le mois en cours. */
export interface InvoiceStatusTotalsQuery {
  readonly from?: string;
  readonly to?: string;
}
