import { StockLocation } from '../../../shared/data-access/demo-catalog';

export type TransferOrigin = 'MORNING' | 'MANUAL' | 'RETURN';
export type TransferStatus = 'IN_TRANSIT' | 'RECEIVED' | 'DISCREPANCY' | 'RETURN';

export const TRANSFER_ORIGINS: readonly TransferOrigin[] = ['MORNING', 'MANUAL', 'RETURN'];

/** Ligne d'un bon : quantité envoyée et quantité reçue (écart si elles diffèrent). */
export interface TransferLineRDto {
  readonly reference: string;
  readonly name: string;
  readonly quantity: number;
  readonly received: number;
}

/** Ligne de la liste des transferts (TRF-01). */
export interface TransferRDto {
  readonly id: string;
  readonly number: string;
  readonly from: StockLocation;
  readonly to: StockLocation;
  readonly origin: TransferOrigin;
  readonly lineCount: number;
  readonly unitCount: number;
  readonly when: string;
  readonly status: TransferStatus;
}

/** Bon de transfert (TRF-04) : les premières lignes, le reste résumé. */
export interface TransferDetailRDto extends TransferRDto {
  readonly operator: string;
  readonly lines: readonly TransferLineRDto[];
  readonly hiddenLines: number;
}
