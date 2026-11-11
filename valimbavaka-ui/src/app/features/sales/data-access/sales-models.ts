import { PaymentMethod } from '../../../shared/models/commerce';
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

export interface InvoiceLineRDto {
  readonly label: string;
  readonly quantity: number;
  readonly total: number;
}

/**
 * Facture d'une vente (FAC-01 à FAC-05) : document légal, jamais supprimé (avoir motivé). `number` et `issuedAt` sont
 * nuls tant que la facture n'est pas émise (aperçu construit depuis la vente, en attendant FAC-01).
 */
export interface InvoiceRDto {
  readonly number: string | null;
  readonly issuedAt: string | null;
  readonly status: InvoiceStatus;
  readonly saleNumber: string;
  readonly channelLabel: string | null;
  readonly sellerName: string | null;
  readonly payment: PaymentMethod;
  readonly issuerName: string;
  readonly issuerNif: string | null;
  readonly lines: readonly InvoiceLineRDto[];
  readonly subtotal: number;
  readonly discountLabel: string | null;
  readonly discount: number;
  readonly total: number;
}
