import { StockLocation } from '../../../shared/data-access/demo-catalog';
import { PageQuery } from '../../../shared/models/page-response';

/** Origine d'un transfert, miroir de `TransferOrigin` côté serveur. */
export type TransferOrigin = 'MORNING' | 'MANUAL' | 'RETURN';
/** Statut d'un transfert côté serveur (TRF-03) : `RETURN` pour un retour de stock, `VALIDATED` sinon. */
export type TransferStatus = 'VALIDATED' | 'RETURN';
/** Statuts de la maquette du détail (TRF-04, données de démonstration) ; la liste utilise `TransferStatus`. */
export type TransferRowStatus = 'IN_TRANSIT' | 'RECEIVED' | 'DISCREPANCY' | 'RETURN';

export const TRANSFER_ORIGINS: readonly TransferOrigin[] = ['MORNING', 'MANUAL', 'RETURN'];

/** Ligne d'un bon : quantité envoyée et quantité reçue (écart si elles diffèrent). */
export interface TransferDetailLineRDto {
  readonly reference: string;
  readonly name: string;
  readonly quantity: number;
  readonly received: number;
}

/** Bon de la maquette du détail (TRF-04), données de démonstration. */
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

/**
 * Ligne de l'historique (`GET /api/v1/transfers`, permission `transfers.read`, TRF-01) : référence, date
 * (`createdAt`, instant UTC), trajet, origine, statut et émetteur. Un libellé d'emplacement vaut `null` si
 * l'emplacement a été désactivé depuis ; `createdByName` vaut « Système » pour un transfert généré hors requête.
 */
export interface TransferSummaryRDto {
  readonly id: string;
  readonly number: string;
  readonly origin: TransferOrigin;
  readonly status: TransferStatus;
  readonly fromLocationId: string;
  readonly fromLocationLabel: string | null;
  readonly toLocationId: string;
  readonly toLocationLabel: string | null;
  readonly lineCount: number;
  readonly quantity: number;
  readonly createdAt: string;
  readonly createdBy: string | null;
  readonly createdByName: string;
}

/** Paramètres de l'historique : pagination commune et filtre facultatif par origine. */
export interface TransferListQuery extends PageQuery {
  readonly origin?: TransferOrigin;
}
