import { PageQuery } from '../../../shared/models/page-response';

/** Miroir de CommissionBase : chiffre d'affaires de la vente (après remises) ou marge brute. */
export type CommissionBase = 'REVENUE' | 'MARGIN';

export const COMMISSION_BASES: readonly CommissionBase[] = ['REVENUE', 'MARGIN'];

/** Bornes du taux acceptées par le serveur (`CommissionRuleWDto`). */
export const MIN_COMMISSION_RATE = 0;
export const MAX_COMMISSION_RATE = 100;
export const MAX_RULE_NAME_LENGTH = 100;

/** Miroir de CommissionRuleRDto : règle de commission d'un canal de vente (COM-01). */
export interface CommissionRuleRDto {
  id: string;
  name: string;
  salesChannelId: string;
  /** Taux en pourcentage, 2 décimales. */
  rate: number;
  base: CommissionBase;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  /** Version lue : renvoyée à la modification pour refuser (409) l'écrasement d'un changement concurrent. */
  version: number;
}

/** Miroir de CommissionRuleWDto : création et modification (ni id ni statut). */
export interface CommissionRuleWDto {
  name: string;
  salesChannelId: string;
  rate: number;
  base: CommissionBase;
  /** Version lue à l'ouverture de la fiche ; absente en création. */
  version?: number | null;
}

/** Miroir de CommissionRuleStatusWDto. */
export interface CommissionRuleStatusWDto {
  active: boolean;
}

/** Filtres de GET /api/v1/commission-rules : pagination, recherche sur le nom, canal et statut. */
export interface CommissionRuleListQuery extends PageQuery {
  salesChannelId?: string;
  active?: boolean;
}
