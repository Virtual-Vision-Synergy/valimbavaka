import { PaymentMethod, SaleChannel } from '../../../shared/models/commerce';

/** Statut de facturation d'une vente (miroir de l'énumération backend InvoiceStatus). */
export type InvoiceStatus = 'PAID' | 'SENT' | 'PENDING' | 'CREDITED';

/** Vente du jour (VTE-07). */
export interface SaleRDto {
  readonly id: string;
  readonly number: string;
  readonly time: string;
  readonly channel: SaleChannel;
  readonly sellerName: string;
  readonly payment: PaymentMethod;
  readonly amount: number;
  readonly status: InvoiceStatus;
  readonly invoiceNumber: string;
}

/** Totaux d'un canal pour la journée. */
export interface ChannelTotalRDto {
  readonly channel: SaleChannel;
  readonly amount: number;
  readonly count: number;
}

/** Journée de ventes : liste complète (la pagination est faite par l'API à terme) et totaux. */
export interface SalesDayRDto {
  readonly date: string;
  readonly total: number;
  readonly count: number;
  readonly byChannel: readonly ChannelTotalRDto[];
  readonly sales: readonly SaleRDto[];
}

export interface InvoiceLineRDto {
  readonly label: string;
  readonly quantity: number;
  readonly total: number;
}

/** Facture émise (FAC-01 à FAC-05) : document légal, jamais supprimé (avoir motivé). */
export interface InvoiceRDto {
  readonly number: string;
  readonly issuedAt: string;
  readonly status: InvoiceStatus;
  readonly saleNumber: string;
  readonly channel: SaleChannel;
  readonly sellerName: string;
  readonly payment: PaymentMethod;
  readonly issuerName: string;
  readonly issuerNif: string | null;
  readonly lines: readonly InvoiceLineRDto[];
  readonly subtotal: number;
  readonly discountLabel: string | null;
  readonly discount: number;
  readonly total: number;
}
