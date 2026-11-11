/** Une gamme de la répartition du CA du jour (DSH-04) : CA de ses lignes et part du CA de toutes les gammes. */
export interface RangeShareRDto {
  readonly rangeId: string;
  /** Code stable de la gamme (`BOTANIKA`, `ZEZIKA`, `SOLIKA`…) : sert à la couleur. */
  readonly code: string;
  readonly label: string;
  readonly revenue: number;
  /** Pourcentage à une décimale ; les parts d'un axe font 100,0 au total (0,0 sans CA). */
  readonly sharePercent: number;
}

/** Un canal de la répartition du CA du jour (DSH-04) : CA de ses ventes validées et part du CA de tous les canaux. */
export interface ChannelShareRDto {
  readonly salesChannelId: string;
  /** `null` pour un canal désactivé depuis. */
  readonly label: string | null;
  readonly revenue: number;
  readonly sharePercent: number;
}

/**
 * Répartition du CA du jour par gamme et par canal, lue sur `GET /api/v1/dashboard/revenue-split` (administrateur).
 * Un CA nul donne des listes présentes, toutes les parts à 0.
 */
export interface RevenueSplitRDto {
  /** Date ISO (yyyy-MM-dd) de la journée lue. */
  readonly date: string;
  readonly byRange: readonly RangeShareRDto[];
  readonly byChannel: readonly ChannelShareRDto[];
}
