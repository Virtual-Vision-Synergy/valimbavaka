import { ProductRange } from '../../../shared/models/tone';

/** Durée de la période d'un rapport (RPT-01) : le jour, le mois ou l'année civile qui contient la date. */
export type ReportGranularity = 'DAY' | 'MONTH' | 'YEAR';

/** Paramètres de `GET /api/v1/reports` (RPT-01) : tout est facultatif (mois en cours, tous canaux, toutes gammes). */
export interface ReportQuery {
  readonly granularity?: ReportGranularity;
  /** Un jour de la période, `yyyy-MM-dd` (aujourd'hui par défaut). */
  readonly date?: string;
  readonly salesChannelId?: string;
  readonly rangeId?: string;
}

/** Indicateurs de ventes d'une période (RPT-01) : CA, ventes validées et panier moyen. */
export interface ReportSalesRDto {
  readonly revenue: number;
  readonly salesCount: number;
  readonly averageBasket: number;
}

/** Rapport d'une période (RPT-01) : période effective `[from, to]`, filtres appliqués et indicateurs de ventes. */
export interface ReportRDto {
  readonly granularity: ReportGranularity;
  readonly from: string;
  readonly to: string;
  readonly salesChannelId: string | null;
  readonly rangeId: string | null;
  readonly sales: ReportSalesRDto;
}

/** Filtre de la démonstration des sections RPT-02 à RPT-05, branchées une à une par leurs tickets. */
export interface ReportDemoFilter {
  readonly granularity: ReportGranularity;
}

export interface ReportKpiRDto {
  readonly value: number;
  readonly previous: number;
}

/** Indicateurs de la période comparés à la précédente (RPT-02). */
export interface ReportKpisRDto {
  readonly revenue: ReportKpiRDto;
  readonly sales: ReportKpiRDto;
  readonly basket: ReportKpiRDto;
  /** Taux de livraisons réussies, en pourcentage. */
  readonly delivery: ReportKpiRDto;
}

/** Un point de la courbe de chiffre d'affaires (RPT-03). */
export interface ReportPointRDto {
  readonly label: string;
  readonly current: number;
  readonly previous: number;
  readonly today: boolean;
}

export interface ReportRangeRDto {
  readonly range: ProductRange;
  readonly amount: number;
}

export interface ReportTopArticleRDto {
  readonly name: string;
  readonly units: number;
  readonly revenue: number;
}

/** Sections de démonstration (RPT-02 à RPT-05) : remplacées par l'API ticket par ticket. */
export interface ReportDemoRDto {
  readonly kpis: ReportKpisRDto;
  readonly points: readonly ReportPointRDto[];
  readonly ranges: readonly ReportRangeRDto[];
  readonly top: readonly ReportTopArticleRDto[];
}
