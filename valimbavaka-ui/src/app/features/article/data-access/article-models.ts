import { PageQuery } from '../../../shared/models/page-response';

/** Miroir de ArticleType côté backend : matière première (MP), semi-fini (SF) ou produit fini. */
export type ArticleType = 'RAW_MATERIAL' | 'SEMI_FINISHED' | 'FINISHED';

/** Miroir de ArticleUnit côté backend. */
export type ArticleUnit = 'PIECE' | 'KILOGRAM' | 'LITER';

/** Miroir de ArticleRDto : fiche article en lecture. */
export interface ArticleRDto {
  id: string;
  /** Code attribué par le serveur (ART-0031), jamais saisi. */
  code: string;
  name: string;
  barcode: string | null;
  type: ArticleType;
  /** Gamme (référentiel SYS-05) : renseignée uniquement pour un produit fini. */
  rangeId: string | null;
  unit: ArticleUnit;
  cost: number | null;
  /** Prix de vente : renseigné uniquement pour un produit fini. */
  price: number | null;
  threshold: number;
  supplierId: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  /** Version lue : renvoyée à la modification pour refuser (409) l'écrasement d'un changement concurrent. */
  version: number;
}

/** Miroir de ArticleWDto : création et modification (ni id, ni code, ni statut). */
export interface ArticleWDto {
  name: string;
  barcode: string | null;
  type: ArticleType;
  rangeId: string | null;
  unit: ArticleUnit;
  cost: number | null;
  price: number | null;
  threshold: number;
  supplierId: string | null;
  /** Version lue lors de l'ouverture de la fiche ; absente en création. */
  version?: number | null;
}

/** Filtres de GET /api/v1/articles : pagination, recherche (nom, code, code-barres) et critères. */
export interface ArticleListQuery extends PageQuery {
  type?: ArticleType;
  rangeId?: string;
  supplierId?: string;
  active?: boolean;
}

/** Miroir de ArticleStatusWDto. */
export interface ArticleStatusWDto {
  active: boolean;
}
