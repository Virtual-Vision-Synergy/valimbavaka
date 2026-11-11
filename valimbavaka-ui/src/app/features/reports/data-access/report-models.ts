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

/**
 * Variation en % (une décimale, négative en cas de baisse) de chaque indicateur par rapport à la période précédente
 * (RPT-02). `null` quand la valeur précédente vaut 0 : pas de pourcentage.
 */
export interface ReportVariationRDto {
  readonly revenue: number | null;
  readonly salesCount: number | null;
  readonly averageBasket: number | null;
}

/** Comparaison à la période précédente (RPT-02) : sa période `[from, to]`, ses indicateurs et les variations. */
export interface ReportComparisonRDto {
  readonly from: string;
  readonly to: string;
  readonly sales: ReportSalesRDto;
  readonly variation: ReportVariationRDto;
}

/** Rapport d'une période (RPT-01, RPT-02) : période effective `[from, to]`, filtres, indicateurs et comparaison. */
export interface ReportRDto {
  readonly granularity: ReportGranularity;
  readonly from: string;
  readonly to: string;
  readonly salesChannelId: string | null;
  readonly rangeId: string | null;
  readonly sales: ReportSalesRDto;
  readonly comparison: ReportComparisonRDto;
}

/** Filtre de la démonstration des sections RPT-03 à RPT-05, branchées une à une par leurs tickets. */
export interface ReportDemoFilter {
  readonly granularity: ReportGranularity;
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

/** Sections de démonstration (RPT-03 à RPT-05) : remplacées par l'API ticket par ticket. */
export interface ReportDemoRDto {
  readonly points: readonly ReportPointRDto[];
  readonly ranges: readonly ReportRangeRDto[];
  readonly top: readonly ReportTopArticleRDto[];
}
