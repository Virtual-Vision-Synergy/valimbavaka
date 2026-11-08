import { PageQuery } from '../../../shared/models/page-response';

/** Bornes acceptées par le serveur (`SalonServiceWDto`, SRV-01). */
export const MAX_SALON_SERVICE_NAME_LENGTH = 100;
export const MIN_DURATION_MINUTES = 1;
export const MAX_DURATION_MINUTES = 600;
export const MAX_COMMISSION_RATE = 100;
export const MAX_CONSUMED_PRODUCTS = 20;

/** Miroir de ConsumedProductRDto : produit habituellement consommé, quantité dans l'unité de l'article. */
export interface ConsumedProductRDto {
  articleId: string;
  quantity: number;
}

/** Miroir de ConsumedProductWDto. */
export interface ConsumedProductWDto {
  articleId: string;
  quantity: number;
}

/** Miroir de SalonServiceRDto : service du catalogue salon (SRV-01). */
export interface SalonServiceRDto {
  id: string;
  name: string;
  durationMinutes: number;
  /** Prix en ariary, 2 décimales. */
  price: number;
  /** Taux de commission propre en %, prioritaire sur le canal et la vendeuse ; null = aucun. */
  commissionRate: number | null;
  /** Produits habituellement consommés, triés par identifiant d'article. */
  products: ConsumedProductRDto[];
  active: boolean;
  createdAt: string;
  updatedAt: string;
  /** Version lue : renvoyée à la modification pour refuser (409) l'écrasement d'un changement concurrent. */
  version: number;
}

/** Miroir de SalonServiceWDto : création et modification (ni id ni statut). */
export interface SalonServiceWDto {
  name: string;
  durationMinutes: number;
  price: number;
  commissionRate: number | null;
  products: ConsumedProductWDto[];
  /** Version lue à l'ouverture de la fiche ; absente en création. */
  version?: number | null;
}

/** Miroir de SalonServiceStatusWDto. */
export interface SalonServiceStatusWDto {
  active: boolean;
}

/** Filtres de GET /api/v1/salon-services : pagination, recherche sur le nom et statut. */
export interface SalonServiceListQuery extends PageQuery {
  active?: boolean;
}
