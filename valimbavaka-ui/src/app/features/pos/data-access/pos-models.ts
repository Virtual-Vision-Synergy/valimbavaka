import { ProductRange } from '../../../shared/models/tone';

import { PaymentMethod, SaleChannel } from '../../../shared/models/commerce';

export type { PaymentMethod, SaleChannel } from '../../../shared/models/commerce';

/** Filtre du catalogue de caisse. */
export type PosCategory = 'ALL' | 'FAVORITES' | ProductRange | 'BOXES';

/** Article vendable au poste de caisse (stock de l'emplacement du poste). */
export interface PosArticleRDto {
  readonly id: string;
  readonly reference: string;
  readonly barcode: string;
  readonly name: string;
  readonly price: number;
  readonly range: ProductRange;
  readonly stock: number;
  readonly favorite: boolean;
  readonly box: boolean;
}

/**
 * Règle de remise d'une promotion (PRO-01) : `PERCENT` (value %), `AMOUNT` (value Ar), `BUY_X` (une unité
 * offerte par groupe de `value` achetées).
 */
export type PosPromotionKind = 'PERCENT' | 'AMOUNT' | 'BUY_X';

/** Promotion automatique du jour, appliquée aux articles ciblés sur les canaux listés (PRO-02). */
export interface PosPromotionRDto {
  readonly id: string;
  readonly name: string;
  readonly kind: PosPromotionKind;
  readonly value: number;
  readonly articleIds: readonly string[];
  readonly channels: readonly SaleChannel[];
}

/** Ligne d'un ticket ouvert (reprise après coupure ou ticket en attente). */
export interface PosTicketLineRDto {
  readonly articleId: string;
  readonly quantity: number;
}

/** Vendeuse à qui la vente est attribuée (commission). */
export interface PosSeller {
  readonly id: string;
  readonly name: string;
}

/**
 * Contexte du poste : emplacement, caissière, vendeuses, canaux (identifiants du serveur), catalogue,
 * promotions automatiques du jour et ticket en cours.
 */
export interface PosContextRDto {
  readonly location: string;
  readonly cashierName: string;
  readonly sellers: readonly PosSeller[];
  /** Vendeuse proposée d'office : le compte connecté s'il est vendeuse, sinon aucune. */
  readonly defaultSellerId: string | null;
  /** Identifiant du canal de vente du serveur pour chaque canal du poste ; absent si le canal est inactif. */
  readonly channelIds: Readonly<Partial<Record<SaleChannel, string>>>;
  readonly ticketNumber: number;
  readonly openedAt: string;
  readonly heldTickets: number;
  readonly articles: readonly PosArticleRDto[];
  readonly promotions: readonly PosPromotionRDto[];
  readonly openTicket: readonly PosTicketLineRDto[];
  readonly openTicketReceived: number;
}

/** Canal de vente actif et emplacement de stock dont il sort (`GET /api/v1/sales/form`, VTE-01). */
export interface SaleChannelOptionRDto {
  readonly id: string;
  readonly code: string;
  readonly label: string;
  readonly stockLocationId: string;
}

/** Vendeuse active proposée par le formulaire de vente. */
export interface SaleSellerRDto {
  readonly id: string;
  readonly firstName: string;
  readonly lastName: string;
  readonly salesChannelId: string | null;
}

/** Article vendable : produit fini actif avec un prix ; `stock` = quantité par emplacement (id → quantité). */
export interface SaleArticleRDto {
  readonly id: string;
  readonly code: string;
  readonly barcode: string | null;
  readonly name: string;
  readonly rangeId: string | null;
  readonly price: number;
  readonly stock: Readonly<Record<string, number>>;
}

/** Promotion automatique utilisable aujourd'hui (PRO-02) : articles atteints et canaux où elle s'applique. */
export interface SalePromotionRDto {
  readonly id: string;
  readonly name: string;
  readonly kind: PosPromotionKind;
  readonly value: number;
  readonly articleIds: readonly string[];
  readonly channelIds: readonly string[];
}

/** Formulaire de vente (VTE-01) ; `nextNumber` est indicatif, le numéro réel est donné à l'enregistrement. */
export interface SaleFormRDto {
  readonly nextNumber: number;
  readonly channels: readonly SaleChannelOptionRDto[];
  readonly sellers: readonly SaleSellerRDto[];
  readonly articles: readonly SaleArticleRDto[];
  readonly promotions: readonly SalePromotionRDto[];
}

/** Ligne vendue : jamais de prix, le serveur lit celui de la fiche article. */
export interface SaleLineWDto {
  readonly articleId: string;
  readonly quantity: number;
}

/** Vente saisie au poste (`POST /api/v1/sales`). */
export interface SaleWDto {
  readonly salesChannelId: string;
  readonly sellerId: string;
  readonly paymentMethod: PaymentMethod;
  readonly lines: readonly SaleLineWDto[];
  /** Code promo saisi (VTE-04) : une demande, le serveur le vérifie et calcule la remise. Absent sans code. */
  readonly promoCode?: string;
}

export type SaleStatus = 'VALIDATED' | 'CANCELLED' | 'REVERSED';

export interface SaleLineRDto {
  readonly articleId: string;
  readonly quantity: number;
  readonly unitPrice: number;
  readonly lineTotal: number;
}

/** Vente enregistrée : prix figés sur les lignes, statut, totaux. */
export interface SaleRDto {
  readonly id: string;
  readonly number: string;
  readonly businessDayId: string;
  readonly businessDate: string;
  readonly salesChannelId: string;
  readonly stockLocationId: string;
  /** `null` : vente d'un autre module sans vendeuse créditée (LIV-03, commande saisie par l'administrateur). */
  readonly sellerId: string | null;
  readonly paymentMethod: PaymentMethod;
  readonly status: SaleStatus;
  readonly subtotal: number;
  readonly discountAmount: number;
  /** Promotion qui a donné `discountAmount` (PRO-02) ; `null` sans remise. */
  readonly promotionId: string | null;
  readonly total: number;
  readonly lines: readonly SaleLineRDto[];
  readonly createdAt: string;
  readonly createdBy: string | null;
}
