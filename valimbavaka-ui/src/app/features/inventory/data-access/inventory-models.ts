import { StockLocation } from '../../../shared/data-access/demo-catalog';

export type InventoryStatus = 'IN_PROGRESS' | 'CLOSED';

/** Motif d'un écart d'inventaire (INV-03). */
export type InventoryReason = 'BREAKAGE' | 'LOSS' | 'ENTRY_ERROR' | 'UNDECLARED_SHOOTING' | 'OTHER';

export const INVENTORY_REASONS: readonly InventoryReason[] = [
  'BREAKAGE',
  'LOSS',
  'ENTRY_ERROR',
  'UNDECLARED_SHOOTING',
  'OTHER',
];

export interface InventoryLineRDto {
  readonly articleId: string;
  readonly reference: string;
  readonly barcode: string;
  readonly name: string;
  readonly theoretical: number;
  readonly unitCost: number;
  readonly counted: number | null;
  readonly reason: InventoryReason | null;
}

export interface InventorySessionRDto {
  readonly number: string;
  readonly location: StockLocation;
  /** Horodatage local `AAAA-MM-JJTHH:MM`. */
  readonly openedAt: string;
  readonly openedBy: string;
  readonly status: InventoryStatus;
  readonly lines: readonly InventoryLineRDto[];
}

export interface PreviousSessionRDto {
  readonly number: string;
  readonly location: StockLocation;
  readonly closedOn: string;
  readonly gapUnits: number;
}

export interface InventoryContextRDto {
  readonly session: InventorySessionRDto;
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
