import { ProductRange } from '../../../shared/models/tone';
import { SaleChannel } from '../../../shared/models/commerce';

export type ReportPeriod = 'DAY' | 'MONTH' | 'YEAR';

/** Filtres du rapport (RPT-01) : `ALL` = pas de restriction. */
export interface ReportFilterWDto {
  readonly period: ReportPeriod;
  readonly channel: SaleChannel | 'ALL';
  readonly range: ProductRange | 'ALL';
  readonly seller: string;
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

export interface ReportRDto {
  readonly kpis: ReportKpisRDto;
  readonly points: readonly ReportPointRDto[];
  readonly ranges: readonly ReportRangeRDto[];
  readonly top: readonly ReportTopArticleRDto[];
}
