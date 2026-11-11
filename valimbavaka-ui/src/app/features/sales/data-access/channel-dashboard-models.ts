/** Canal du tableau de bord (VTE-08, miroir de `ChannelSalesRDto`) : libellé nul pour un canal désactivé depuis. */
export interface ChannelSalesRDto {
  readonly salesChannelId: string;
  readonly salesChannelLabel: string | null;
  readonly revenue: number;
  readonly salesCount: number;
  readonly revenueSharePercent: number;
}

/** Tableau de bord par canal sur les jours ouvrés `from` à `to` inclus (miroir de `ChannelDashboardRDto`). */
export interface ChannelDashboardRDto {
  readonly from: string;
  readonly to: string;
  readonly totalRevenue: number;
  readonly totalSalesCount: number;
  readonly channels: readonly ChannelSalesRDto[];
}

/** Période de `GET /api/v1/sales/channel-dashboard` : absente, le serveur prend le mois en cours. */
export interface ChannelDashboardQuery {
  readonly from?: string;
  readonly to?: string;
}
