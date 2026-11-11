import { ProductRange } from '../../../shared/models/tone';

export type SalesChannel = 'SHOP' | 'FACEBOOK' | 'PHONE';
export type DayStepCode = 'OPENING' | 'SALES' | 'DELIVERIES' | 'CLOSING';
export type ActivityKind =
  'SALE' | 'DELIVERY' | 'DELIVERY_FAILED' | 'SERVICE' | 'SHOOTING' | 'TRANSFER';

/** Étape de la journée commerciale (JR-01 → JR-06). */
export interface DayStepRDto {
  readonly code: DayStepCode;
  readonly state: 'done' | 'current' | 'upcoming';
  readonly params: Readonly<Record<string, string | number>>;
}

/** Statut de la journée du jour ; `null` tant qu'elle n'est pas ouverte (JR-01). */
export type BusinessDayStatus = 'OPEN' | 'CLOSED';

/** Chiffre d'affaires du jour et de la veille ; `changePercent` est `null` sans comparaison possible. */
export interface RevenueKpiRDto {
  readonly amount: number;
  readonly previousAmount: number;
  readonly changePercent: number | null;
}

/** Un canal de vente (VTE-08) : chiffre d'affaires, ventes validées et part du total en pourcent. */
export interface ChannelSalesRDto {
  readonly salesChannelId: string;
  /** `null` pour un canal désactivé depuis. */
  readonly salesChannelLabel: string | null;
  readonly revenue: number;
  readonly salesCount: number;
  readonly revenueSharePercent: number;
}

export interface SalesKpiRDto {
  readonly count: number;
  readonly previousCount: number;
  readonly averageBasket: number;
  readonly channels: readonly ChannelSalesRDto[];
}

/** Commandes du jour ; `total` ne compte pas les annulées (`cancelled`). */
export interface DeliveryKpiRDto {
  readonly total: number;
  readonly delivered: number;
  readonly onRound: number;
  readonly toPrepare: number;
  readonly failed: number;
  readonly cancelled: number;
}

/** Écart de la réconciliation de clôture : somme signée des unités et nombre d'articles concernés. */
export interface StockGapKpiRDto {
  readonly units: number;
  readonly articles: number;
}

export interface DailyRevenueRDto {
  /** Date ISO (yyyy-MM-dd). */
  readonly date: string;
  readonly amount: number;
}

export interface RangeRevenueRDto {
  readonly range: ProductRange;
  readonly amount: number;
}

export interface ChannelRevenueRDto {
  readonly channel: SalesChannel;
  readonly amount: number;
}

export interface ActivityRDto {
  readonly id: string;
  readonly kind: ActivityKind;
  readonly actor: string;
  readonly params: Readonly<Record<string, string | number>>;
  readonly minutesAgo: number;
}

export interface TopArticleRDto {
  readonly reference: string;
  readonly name: string;
  readonly range: ProductRange;
  readonly quantity: number;
  readonly amount: number;
}

export interface SellerPerformanceRDto {
  readonly name: string;
  readonly tickets: number;
  readonly amount: number;
  readonly target: number;
}

/**
 * Les 4 indicateurs du cockpit (DSH-01), lus sur `GET /api/v1/dashboard` (administrateur). `stockGap` est `null` tant
 * que la journée n'est pas ouverte. Les alertes de stock (DSH-03) sont lues à part sur `GET /api/v1/stocks/alerts`.
 */
export interface DashboardRDto {
  readonly businessDate: string;
  readonly status: BusinessDayStatus | null;
  /** Instant ISO de la lecture (heure du serveur). */
  readonly generatedAt: string;
  readonly revenue: RevenueKpiRDto;
  readonly sales: SalesKpiRDto;
  readonly deliveries: DeliveryKpiRDto;
  readonly stockGap: StockGapKpiRDto | null;
}

/**
 * Zones du cockpit encore simulées : graphiques (DSH-02), répartition (DSH-04), flux d'activité, meilleurs articles,
 * vendeuses, objectif mensuel et étapes. Chaque zone sortira d'ici quand son API existera ; elles ne contiennent plus
 * aucun des 4 indicateurs.
 */
export interface DashboardDemoRDto {
  readonly businessDate: string;
  readonly openedAt: string;
  readonly openedBy: string;
  readonly updatedAt: string;
  readonly monthlyTarget: number;
  readonly monthlyRevenue: number;
  readonly steps: readonly DayStepRDto[];
  readonly revenueLast7Days: readonly DailyRevenueRDto[];
  /** Chiffre d'affaires des 4 dernières semaines, `date` = lundi de la semaine. */
  readonly revenueLast4Weeks: readonly DailyRevenueRDto[];
  readonly revenueByRange: readonly RangeRevenueRDto[];
  readonly revenueByChannel: readonly ChannelRevenueRDto[];
  readonly activity: readonly ActivityRDto[];
  readonly topArticles: readonly TopArticleRDto[];
  readonly sellers: readonly SellerPerformanceRDto[];
}

/** Fenêtre du graphique de chiffre d'affaires. */
export type RevenueWindow = 'WEEK' | 'MONTH';
