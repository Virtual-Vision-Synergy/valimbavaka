import { PageQuery } from '../../../shared/models/page-response';
import { ConsumedProductWDto } from './salon-catalog-models';

/** Code de l'emplacement de stock du salon (libellé « Salon ») : les prestations y consomment leurs produits. */
export const SALON_LOCATION_CODE = 'TRADE_SHOW';

/** Bornes acceptées par le serveur (`ServiceRecordWDto`, SRV-02). */
export const MAX_CUSTOMER_NAME_LENGTH = 100;
export const MAX_RECORD_PRODUCTS = 20;

/** Miroir de ServiceRecordProductRDto : produit consommé, code et nom copiés, quantité dans l'unité de l'article. */
export interface ServiceRecordProductRDto {
  readonly articleId: string;
  readonly articleCode: string;
  readonly articleName: string;
  readonly quantity: number;
}

/** Miroir de ServiceRecordRDto : prestation enregistrée (SRV-02), prix et durée copiés du catalogue. */
export interface ServiceRecordRDto {
  readonly id: string;
  /** Numéro attribué par le serveur (`PRE-0001`). */
  readonly number: string;
  readonly salonServiceId: string;
  readonly serviceName: string;
  readonly durationMinutes: number;
  /** Prix en ariary : chiffre d'affaires de la prestation pour la commission (COM-02). */
  readonly price: number;
  readonly customerName: string | null;
  readonly sellerId: string;
  readonly sellerName: string;
  readonly salesChannelId: string;
  /** Emplacement de stock d'où sortent les produits : le salon. */
  readonly locationId: string;
  readonly products: readonly ServiceRecordProductRDto[];
  readonly note: string | null;
  /** Instant ISO de l'enregistrement. */
  readonly performedAt: string;
}

/**
 * Miroir de ServiceRecordWDto. Ni prix, ni vendeuse, ni emplacement : le serveur copie le prix du catalogue, prend le
 * compte connecté et consomme dans le stock salon. `products` absent (`null`) = recette du catalogue.
 */
export interface ServiceRecordWDto {
  readonly salonServiceId: string;
  readonly customerName: string | null;
  readonly products: readonly ConsumedProductWDto[] | null;
  readonly note?: string | null;
}

/** Filtres de GET /api/v1/service-records : pagination et recherche (numéro, service, cliente, vendeuse). */
export type ServiceRecordListQuery = PageQuery;

/**
 * Filtres de GET /api/v1/service-records/mine (SRV-03) : prestations du compte connecté sur un jour (heure de
 * Madagascar), les plus récentes d'abord. `date` au format `AAAA-MM-JJ`, aujourd'hui si absente ; pas de recherche.
 */
export interface ServiceRecordDayQuery extends PageQuery {
  readonly date?: string;
}
