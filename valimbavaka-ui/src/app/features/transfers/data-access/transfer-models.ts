import { StockLocation } from '../../../shared/data-access/demo-catalog';

/** Origine d'un transfert, miroir de `TransferOrigin` côté serveur. */
export type TransferOrigin = 'MORNING' | 'MANUAL' | 'RETURN';
/** Statut d'un transfert côté serveur (TRF-03) : `RETURN` pour un retour de stock, `VALIDATED` sinon. */
export type TransferStatus = 'VALIDATED' | 'RETURN';
/** Statut affiché par la maquette de la liste ; TRF-01 le remplacera par `TransferStatus`. */
export type TransferRowStatus = 'IN_TRANSIT' | 'RECEIVED' | 'DISCREPANCY' | 'RETURN';

export const TRANSFER_ORIGINS: readonly TransferOrigin[] = ['MORNING', 'MANUAL', 'RETURN'];

/** Ligne d'un bon : quantité envoyée et quantité reçue (écart si elles diffèrent). */
export interface TransferDetailLineRDto {
  readonly reference: string;
  readonly name: string;
  readonly quantity: number;
  readonly received: number;
}

/** Ligne de la liste des transferts (TRF-01), données de démonstration. */
export interface TransferRowRDto {
  readonly id: string;
  readonly number: string;
  readonly from: StockLocation;
  readonly to: StockLocation;
  readonly origin: TransferOrigin;
  readonly lineCount: number;
  readonly unitCount: number;
  readonly when: string;
  readonly status: TransferRowStatus;
}

/** Bon de transfert (TRF-04) : les premières lignes, le reste résumé. */
export interface TransferDetailRDto extends TransferRowRDto {
  readonly operator: string;
  readonly lines: readonly TransferDetailLineRDto[];
  readonly hiddenLines: number;
}

/** Ligne saisie d'un transfert : quantité positive, 3 décimales au maximum (`TransferLineWDto`). */
export interface TransferLineWDto {
  readonly articleId: string;
  readonly quantity: number;
}

/** `POST /api/v1/transfers/returns` (permission `transfers.return`) : 1 à 100 lignes, un article une fois. */
export interface StockReturnWDto {
  readonly fromLocationId: string;
  readonly toLocationId: string;
  readonly lines: readonly TransferLineWDto[];
  readonly note: string | null;
}

export interface TransferLineRDto {
  readonly articleId: string;
  readonly quantity: number;
}

/**
 * Transfert enregistré (`TransferRDto` du serveur) : `createdAt` est sa date, `createdBy` son émetteur.
 * Les emplacements sont des identifiants du référentiel (SYS-05).
 */
export interface TransferRDto {
  readonly id: string;
  readonly number: string;
  readonly origin: TransferOrigin;
  readonly status: TransferStatus;
  readonly fromLocationId: string;
  readonly toLocationId: string;
  readonly note: string | null;
  readonly lines: readonly TransferLineRDto[];
  readonly createdAt: string;
  readonly createdBy: string | null;
}
