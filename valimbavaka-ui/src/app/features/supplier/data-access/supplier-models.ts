import { PageQuery } from '../../../shared/models/page-response';

/** Miroir de SupplierType côté backend. */
export type SupplierType = 'LOCAL' | 'FOREIGN';

/** Délai par défaut de chaque type (FRS-04), identique à `SupplierType.defaultLeadTimeDays()` côté serveur. */
export const DEFAULT_LEAD_TIME_DAYS: Readonly<Record<SupplierType, number>> = {
  LOCAL: 7,
  FOREIGN: 30,
};

/** Bornes du délai de réapprovisionnement acceptées par le serveur (`SupplierWDto`). */
export const MAX_LEAD_TIME_DAYS = 365;

/** Miroir de SupplierRDto : fiche fournisseur en lecture. */
export interface SupplierRDto {
  id: string;
  name: string;
  type: SupplierType;
  contactName: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  notes: string | null;
  /** Délai de réapprovisionnement en jours (FRS-04). */
  leadTimeDays: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  /** Version lue : renvoyée à la modification pour refuser (409) l'écrasement d'un changement concurrent. */
  version: number;
}

/** Miroir de SupplierWDto : création et modification (ni id ni statut). */
export interface SupplierWDto {
  name: string;
  type: SupplierType;
  contactName: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  notes: string | null;
  /** Délai en jours (0 à 365) ; `null` = délai par défaut du type. */
  leadTimeDays: number | null;
  /** Version lue lors de l'ouverture de la fiche ; absente en création. */
  version?: number | null;
}

/** Filtres de GET /api/v1/suppliers : pagination, recherche (nom, contact) et type. */
export interface SupplierListQuery extends PageQuery {
  type?: SupplierType;
}

/** Miroir de SupplierStatusWDto. */
export interface SupplierStatusWDto {
  active: boolean;
}
