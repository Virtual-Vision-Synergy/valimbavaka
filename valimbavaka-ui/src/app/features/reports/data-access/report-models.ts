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

/**
 * Un point de la courbe du CA (RPT-03) : un jour (période d'un jour ou d'un mois) ou le premier jour d'un mois
 * (période d'une année), et son chiffre d'affaires (0 sans vente : la série n'a pas de trou).
 */
export interface ReportTrendPointRDto {
  readonly date: string;
  readonly revenue: number;
}

/**
 * CA d'une gamme sur la période (RPT-04) : somme des totaux de ligne de ses articles (avant la remise globale de la
 * vente) et sa part du CA de toutes les gammes, en % à une décimale.
 */
export interface ReportRangeRDto {
  readonly rangeId: string;
  /** Code de la gamme (`BOTANIKA`…) : il donne la couleur (`data-range`). */
  readonly code: string;
  readonly label: string;
  readonly revenue: number;
  readonly share: number;
}

/**
 * Répartition du CA par gamme (RPT-04) : les gammes qui ont vendu, la plus forte d'abord, et la somme de leurs CA.
 * Les parts totalisent exactement 100,0 ; `items` est vide quand rien n'a été vendu. Le filtre gamme est ignoré.
 */
export interface ReportRangesRDto {
  readonly revenue: number;
  readonly items: readonly ReportRangeRDto[];
}

/**
 * Une ligne du classement des articles (RPT-05) : quantité vendue, CA (somme des totaux de ligne, avant la remise
 * globale de la vente), marge brute (CA moins coût d'achat ; `null` si le coût d'une ligne était inconnu à la vente)
 * et part du CA de toutes les lignes de la période, en % à une décimale.
 */
export interface ReportRankedArticleRDto {
  readonly articleId: string;
  readonly code: string;
  readonly name: string;
  readonly quantity: number;
  readonly revenue: number;
  readonly margin: number | null;
  readonly share: number;
}

/**
 * Classement des articles les plus vendus (RPT-05) : au plus `size` articles, le plus fort CA d'abord (égalité :
 * nom puis code). `revenue` est le CA de toutes les lignes de la période et des filtres, base des parts (pas la
 * somme des lignes listées) ; `items` est vide quand rien n'a été vendu. Les trois filtres s'appliquent.
 */
export interface ReportRankingRDto {
  readonly size: number;
  readonly revenue: number;
  readonly items: readonly ReportRankedArticleRDto[];
}

/** Rapport d'une période (RPT-01 à RPT-05) : période, filtres, indicateurs, comparaison, courbe, gammes, classement. */
export interface ReportRDto {
  readonly granularity: ReportGranularity;
  readonly from: string;
  readonly to: string;
  readonly salesChannelId: string | null;
  readonly rangeId: string | null;
  readonly sales: ReportSalesRDto;
  readonly comparison: ReportComparisonRDto;
  readonly trend: readonly ReportTrendPointRDto[];
  readonly ranges: ReportRangesRDto;
  readonly ranking: ReportRankingRDto;
}
