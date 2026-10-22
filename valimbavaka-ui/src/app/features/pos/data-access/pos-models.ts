import { ProductRange } from '../../../shared/models/tone';

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

/** Promotion appliquée automatiquement aux articles ciblés (PRO-02). */
export interface PosPromotionRDto {
  readonly code: string;
  readonly name: string;
  readonly rate: number;
  readonly articleIds: readonly string[];
}

/** Ligne d'un ticket ouvert (reprise après coupure ou ticket en attente). */
export interface PosTicketLineRDto {
  readonly articleId: string;
  readonly quantity: number;
}

/** Contexte du poste : emplacement, caissière, catalogue, promotion active et ticket en cours. */
export interface PosContextRDto {
  readonly location: string;
  readonly cashierName: string;
  readonly ticketNumber: number;
  readonly openedAt: string;
  readonly heldTickets: number;
  readonly articles: readonly PosArticleRDto[];
  readonly promotion: PosPromotionRDto | null;
  readonly openTicket: readonly PosTicketLineRDto[];
  readonly openTicketReceived: number;
}
