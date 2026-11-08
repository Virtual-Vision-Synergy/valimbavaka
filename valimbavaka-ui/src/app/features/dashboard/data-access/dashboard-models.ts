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

export interface RevenueKpiRDto {
  readonly amount: number;
  readonly previousAmount: number;
  readonly changePercent: number;
  readonly dailyTarget: number;
}

export interface SalesKpiRDto {
  readonly tickets: number;
  readonly delta: number;
  readonly averageBasket: number;
  readonly byChannel: Readonly<Record<SalesChannel, number>>;
}

export interface DeliveryKpiRDto {
  readonly done: number;
  readonly total: number;
  readonly inProgress: number;
  readonly failed: number;
  readonly activeRounds: number;
  readonly collected: number;
}

export interface StockGapKpiRDto {
  readonly units: number;
  readonly articles: number;
}

/** Une barre du graphique de CA (DSH-02) : un jour, ou une semaine (`date` = lundi). */
export interface RevenuePointRDto {
  /** Date ISO (yyyy-MM-dd). */
  readonly date: string;
  /** Ventes validées + prestations salon, 0 si rien n'a été vendu. */
  readonly amount: number;
}

/**
 * Tendance du CA (DSH-02), `GET /api/v1/dashboard/revenue-trend` (permission `reports.read`) : 7 jours jusqu'à
 * aujourd'hui inclus, puis 4 semaines du lundi au dimanche, la semaine en cours en dernier.
 */
export interface RevenueTrendRDto {
  readonly today: string;
  readonly last7Days: readonly RevenuePointRDto[];
  readonly last4Weeks: readonly RevenuePointRDto[];
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
 * Cockpit du jour (DSH-01, DSH-04) : agrégat en lecture seule, calculé côté serveur. Les alertes de stock (DSH-03)
 * et la tendance du CA (DSH-02) n'en font pas partie : la page les lit à part (`GET /api/v1/stocks/alerts`,
 * `GET /api/v1/dashboard/revenue-trend`).
 */
export interface DashboardRDto {
  readonly businessDate: string;
  readonly openedAt: string;
  readonly openedBy: string;
  readonly updatedAt: string;
  readonly monthlyTarget: number;
  readonly monthlyRevenue: number;
  readonly steps: readonly DayStepRDto[];
  readonly revenue: RevenueKpiRDto;
  readonly sales: SalesKpiRDto;
  readonly deliveries: DeliveryKpiRDto;
  readonly stockGap: StockGapKpiRDto;
  readonly revenueByRange: readonly RangeRevenueRDto[];
  readonly revenueByChannel: readonly ChannelRevenueRDto[];
  readonly activity: readonly ActivityRDto[];
  readonly topArticles: readonly TopArticleRDto[];
  readonly sellers: readonly SellerPerformanceRDto[];
}

/** Fenêtre du graphique de chiffre d'affaires. */
export type RevenueWindow = 'WEEK' | 'MONTH';
