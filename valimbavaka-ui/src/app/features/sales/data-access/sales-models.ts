import { PaymentMethod, SaleChannel } from '../../../shared/models/commerce';
import { PageQuery } from '../../../shared/models/page-response';

/** Statut d'une vente (miroir de `OperationStatus` backend) : une vente ne se supprime jamais (JNL-02). */
export type SaleStatus = 'VALIDATED' | 'CANCELLED' | 'REVERSED';

/** Article d'une vente listée (miroir de `SaleSummaryLineRDto`) : nom lu sur la fiche, total de ligne figé. */
export interface SaleSummaryLineRDto {
  readonly articleId: string;
  readonly articleName: string | null;
  readonly quantity: number;
  readonly lineTotal: number;
}

/**
 * Vente de la liste du jour (VTE-07, miroir de `SaleSummaryRDto`) : référence, heure (`createdAt`), canal, vendeuse,
 * articles, règlement, promo (`discountAmount`) et total. `salesChannelLabel` est nul pour un canal désactivé depuis,
 * `sellerName` pour une vente créditée à personne (commande livrée saisie par l'administrateur, LIV-03).
 */
export interface SaleSummaryRDto {
  readonly id: string;
  readonly number: string;
  readonly businessDate: string;
  readonly createdAt: string;
  readonly salesChannelId: string;
  readonly salesChannelLabel: string | null;
  readonly sellerId: string | null;
  readonly sellerName: string | null;
  readonly paymentMethod: PaymentMethod;
  readonly status: SaleStatus;
  readonly subtotal: number;
  readonly discountAmount: number;
  readonly total: number;
  readonly lines: readonly SaleSummaryLineRDto[];
}

/**
 * Filtres de `GET /api/v1/sales` : jour `yyyy-MM-dd` (aujourd'hui côté serveur s'il manque), canal et vendeuse.
 * Une vendeuse ne lit que ses ventes : le serveur ignore alors `sellerId`.
 */
export interface SalesQuery extends PageQuery {
  readonly date?: string;
  readonly salesChannelId?: string;
  readonly sellerId?: string;
}

/** Ligne affichée dans le tableau des ventes du jour. */
export interface SaleRowRDto {
  readonly id: string;
  readonly number: string;
  readonly time: string;
  readonly channelLabel: string | null;
  readonly sellerName: string | null;
  readonly articles: string;
  readonly payment: PaymentMethod;
  readonly discount: number;
  readonly total: number;
  readonly status: SaleStatus;
}

/** Statut de facturation d'une vente (miroir de l'énumération backend InvoiceStatus, FAC-01). */
export type InvoiceStatus = 'PAID' | 'SENT' | 'PENDING' | 'CREDITED';

/** Totaux d'un canal pour la journée (maquette VTE-08, pas encore servie par l'API). */
export interface ChannelTotalRDto {
  readonly channel: SaleChannel;
  readonly amount: number;
  readonly count: number;
}

/** Totaux de la journée par canal (maquette VTE-08, pas encore servie par l'API). */
export interface SalesDayRDto {
  readonly date: string;
  readonly total: number;
  readonly count: number;
  readonly byChannel: readonly ChannelTotalRDto[];
}

/** Ligne d'une facture émise (miroir de `InvoiceLineRDto`) : nom de l'article et prix tels qu'à l'émission. */
export interface InvoiceLineRDto {
  readonly articleId: string;
  readonly label: string;
  readonly quantity: number;
  readonly unitPrice: number;
  readonly lineTotal: number;
}

/**
 * Facture émise à l'encaissement d'une vente (FAC-01, miroir de `InvoiceRDto`) : instantané figé de la vente, jamais
 * modifié ni supprimé. `sellerName` est nul pour une vente créditée à personne (commande livrée, LIV-03),
 * `salesChannelLabel` si le canal a été désactivé entre-temps, `issuerNif` tant que le NIF n'est pas configuré.
 */
export interface InvoiceRDto {
  readonly id: string;
  readonly number: string;
  readonly saleId: string;
  readonly saleNumber: string;
  readonly issuedAt: string;
  readonly businessDate: string;
  readonly status: SaleStatus;
  readonly salesChannelLabel: string | null;
  readonly sellerId: string | null;
  readonly sellerName: string | null;
  readonly paymentMethod: PaymentMethod;
  readonly issuerName: string;
  readonly issuerNif: string | null;
  readonly subtotal: number;
  readonly discountAmount: number;
  readonly total: number;
  readonly lines: readonly InvoiceLineRDto[];
}

export interface InvoiceLineView {
  readonly label: string;
  readonly quantity: number;
  readonly total: number;
}

/**
 * Facture telle que le document papier l'affiche : la facture émise par le serveur (`toInvoiceView`) ou, pour une
 * vente qui n'en a pas (antérieure à FAC-01), un aperçu construit depuis la vente (`toInvoicePreview`) sans numéro ni
 * date d'émission. Un avoir motivé (FAC-04) ne se supprime pas : il compense la facture.
 */
export interface InvoiceView {
  readonly number: string | null;
  readonly issuedAt: string | null;
  readonly status: InvoiceStatus;
  readonly saleNumber: string;
  readonly channelLabel: string | null;
  readonly sellerName: string | null;
  readonly payment: PaymentMethod;
  readonly issuerName: string;
  readonly issuerNif: string | null;
  readonly lines: readonly InvoiceLineView[];
  readonly subtotal: number;
  readonly discountLabel: string | null;
  readonly discount: number;
  readonly total: number;
}
