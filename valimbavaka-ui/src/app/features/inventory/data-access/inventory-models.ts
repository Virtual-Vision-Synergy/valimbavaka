import { PageQuery } from '../../../shared/models/page-response';

export type InventoryStatus = 'IN_PROGRESS' | 'CLOSED' | 'CANCELLED';

/** Périmètre d'une session : tous les articles de l'emplacement, ou les produits d'une gamme. */
export type InventoryScope = 'ALL' | 'RANGE';

/** Motif d'un écart d'inventaire (INV-03). */
export type InventoryReason = 'BREAKAGE' | 'LOSS' | 'ENTRY_ERROR' | 'UNDECLARED_SHOOTING' | 'OTHER';

export const INVENTORY_REASONS: readonly InventoryReason[] = [
  'BREAKAGE',
  'LOSS',
  'ENTRY_ERROR',
  'UNDECLARED_SHOOTING',
  'OTHER',
];

/** Ligne de la feuille de comptage affichée, construite à partir d'un `InventoryLineRDto` (INV-02). */
export interface InventorySheetLine {
  readonly lineId: string;
  readonly articleId: string;
  readonly reference: string;
  readonly barcode: string;
  readonly name: string;
  /** Théorique du serveur : figé au comptage, sinon stock actuel de l'emplacement. */
  readonly theoretical: number;
  readonly unitCost: number | null;
  readonly counted: number | null;
  readonly reason: InventoryReason | null;
}

/** Feuille de comptage affichée par la page d'inventaire : la session en cours et toutes ses lignes. */
export interface InventorySheet {
  readonly id: string;
  readonly number: string;
  readonly locationLabel: string;
  /** Horodatage local `AAAA-MM-JJTHH:MM`. */
  readonly openedAt: string;
  /** Responsable du comptage choisi à l'ouverture. */
  readonly openedBy: string;
  readonly status: InventoryStatus;
  readonly lines: readonly InventorySheetLine[];
}

export interface PreviousSessionRDto {
  readonly number: string;
  readonly locationLabel: string;
  readonly closedOn: string;
  readonly gapUnits: number;
}

export interface InventoryContextRDto {
  /** Null quand aucune session n'est en cours. */
  readonly session: InventorySheet | null;
  readonly previous: readonly PreviousSessionRDto[];
}

/** Clôture de la session : comptages et motifs ; le serveur génère les ajustements de stock. */
export interface InventoryCloseWDto {
  readonly number: string;
  readonly lines: readonly {
    readonly articleId: string;
    readonly counted: number;
    readonly reason: InventoryReason | null;
  }[];
}

/** Ouverture d'une session (INV-01) : les 4 informations obligatoires ; `rangeId` seulement si `RANGE`. */
export interface InventorySessionWDto {
  readonly locationId: string;
  readonly scope: InventoryScope;
  readonly rangeId: string | null;
  readonly responsibleId: string;
  /** Date `AAAA-MM-JJ`, aujourd'hui ou plus tard. */
  readonly countDate: string;
}

/** Session d'inventaire du serveur (`InventorySessionRDto`). */
export interface InventorySessionRDto {
  readonly id: string;
  readonly number: string;
  readonly locationId: string;
  /** Code serveur de l'emplacement : `WAREHOUSE`, `STORE`, `DELIVERY`, `TRADE_SHOW`. */
  readonly locationCode: string;
  readonly locationLabel: string;
  readonly scope: InventoryScope;
  readonly rangeId: string | null;
  readonly responsibleId: string;
  readonly responsibleName: string;
  readonly countDate: string;
  readonly status: InventoryStatus;
  readonly openedAt: string;
  readonly openedBy: string | null;
  readonly closedAt: string | null;
  readonly lineCount: number;
  readonly version: number;
}

/** Ligne d'une session du serveur (`InventoryLineRDto`) ; `gap` = compté − théorique, null si non compté. */
export interface InventoryLineRDto {
  readonly id: string;
  readonly articleId: string;
  readonly articleCode: string;
  readonly articleName: string;
  readonly barcode: string | null;
  readonly unitCost: number | null;
  readonly theoreticalQuantity: number;
  readonly countedQuantity: number | null;
  readonly gap: number | null;
  readonly reason: InventoryReason | null;
}

/** Filtres de la liste des sessions (`GET /api/v1/inventories`). */
export interface InventorySessionQuery extends PageQuery {
  readonly status?: InventoryStatus;
  readonly locationId?: string;
}

/** Comptage physique d'une ligne (INV-02) : le serveur fige le théorique et calcule l'écart. */
export interface InventoryCountWDto {
  readonly countedQuantity: number;
}
