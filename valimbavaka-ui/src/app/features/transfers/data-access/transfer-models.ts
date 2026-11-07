import { PageQuery } from '../../../shared/models/page-response';
import { ArticleUnit } from '../../article/data-access/article-models';

/** Origine d'un transfert, miroir de `TransferOrigin` côté serveur. */
export type TransferOrigin = 'MORNING' | 'MANUAL' | 'RETURN';
/** Statut d'un transfert côté serveur (TRF-03) : `RETURN` pour un retour de stock, `VALIDATED` sinon. */
export type TransferStatus = 'VALIDATED' | 'RETURN';

export const TRANSFER_ORIGINS: readonly TransferOrigin[] = ['MORNING', 'MANUAL', 'RETURN'];

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

/** Ligne du détail (`TransferDetailLineRDto`) : l'article (code, nom et unité actuels) et la quantité déplacée. */
export interface TransferDetailLineRDto {
  readonly articleId: string;
  readonly articleCode: string;
  readonly articleName: string;
  readonly unit: ArticleUnit;
  readonly quantity: number;
}

/**
 * Détail d'un transfert (`GET /api/v1/transfers/{id}`, permission `transfers.read`, TRF-04) : la ligne de
 * l'historique, la note et toutes les lignes dans l'ordre de saisie ; `quantity` est la somme des lignes.
 */
export interface TransferDetailRDto {
  readonly id: string;
  readonly number: string;
  readonly origin: TransferOrigin;
  readonly status: TransferStatus;
  readonly fromLocationId: string;
  readonly fromLocationLabel: string | null;
  readonly toLocationId: string;
  readonly toLocationLabel: string | null;
  readonly note: string | null;
  readonly lines: readonly TransferDetailLineRDto[];
  readonly quantity: number;
  readonly createdAt: string;
  readonly createdBy: string | null;
  readonly createdByName: string;
}
