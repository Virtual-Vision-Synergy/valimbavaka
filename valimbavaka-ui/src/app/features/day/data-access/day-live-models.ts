import { DayStatus } from './day-models';

/** Canal de vente du jour (serveur, VTE-08) : CA, nombre de ventes validées et part du CA total. */
export interface DayChannelSalesRDto {
  readonly salesChannelId: string;
  readonly salesChannelLabel: string | null;
  readonly revenue: number;
  readonly salesCount: number;
  readonly revenueSharePercent: number;
}

/** Ventes validées du jour : les ventes annulées ou contre-passées ne comptent pas. */
export interface DaySalesRDto {
  readonly revenue: number;
  readonly count: number;
  readonly channels: readonly DayChannelSalesRDto[];
}

/** Commandes à livrer prévues aujourd'hui, par statut. */
export interface DayDeliveriesRDto {
  readonly total: number;
  readonly toPrepare: number;
  readonly onRound: number;
  readonly delivered: number;
  readonly failed: number;
  readonly cancelled: number;
}

/** Prestations salon réalisées aujourd'hui : nombre et chiffre d'affaires. */
export interface DayServicesRDto {
  readonly count: number;
  readonly revenue: number;
}

export type DayFeedType = 'SALE' | 'DELIVERY' | 'SERVICE';

/**
 * Opération du flux : `reference` = numéro de la vente, de la commande ou de la prestation ; `label` = canal, client
 * ou service ; `status` = statut d'une vente ou d'une livraison (`null` pour une prestation) ; `occurredAt` = heure.
 */
export interface DayFeedItemRDto {
  readonly type: DayFeedType;
  readonly id: string;
  readonly reference: string;
  readonly label: string | null;
  readonly amount: number | null;
  readonly status: string | null;
  readonly actorName: string | null;
  readonly occurredAt: string;
}

/**
 * Vue en direct de la journée (`GET /api/v1/days/live`, JR-03) ; `status` vaut `null` si la journée n'est pas
 * ouverte.
 */
export interface DayLiveRDto {
  readonly businessDate: string;
  readonly status: DayStatus | null;
  readonly generatedAt: string;
  readonly sales: DaySalesRDto;
  readonly deliveries: DayDeliveriesRDto;
  readonly services: DayServicesRDto;
  readonly feed: readonly DayFeedItemRDto[];
}
