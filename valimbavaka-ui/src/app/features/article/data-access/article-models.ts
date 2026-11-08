import { PageQuery } from '../../../shared/models/page-response';
import { ProductionBatchStatus } from '../../production/data-access/production-models';
import { PromotionRDto } from '../../promotions/data-access/promotion-models';
import { StockMovementRDto, StockRDto } from '../../stocks/data-access/stock-models';

/** Miroir de ArticleType côté backend : matière première (MP), semi-fini (SF) ou produit fini. */
export type ArticleType = 'RAW_MATERIAL' | 'SEMI_FINISHED' | 'FINISHED';

/** Miroir de ProductionUsage (ART-04) : INPUT = MP et SF entrants, OUTPUT = produits finis sortants. */
export type ProductionUsage = 'INPUT' | 'OUTPUT';

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

/** Critères communs à la liste et à l'export : recherche (nom, code, code-barres) et filtres. */
export interface ArticleExportQuery {
  search?: string;
  type?: ArticleType;
  rangeId?: string;
  supplierId?: string;
  active?: boolean;
}

/** Filtres de GET /api/v1/articles : pagination et critères. */
export interface ArticleListQuery extends PageQuery, ArticleExportQuery {}

/** Miroir de ArticleStatusWDto. */
export interface ArticleStatusWDto {
  active: boolean;
}

/** Miroir de ImportConflictMode (ART-07) : SKIP rejette la ligne en conflit, OVERWRITE remplace l'article existant. */
export type ImportConflictMode = 'SKIP' | 'OVERWRITE';

/** Miroir de ImportRowStatus (ART-07). */
export type ImportRowStatus = 'CREATED' | 'UPDATED' | 'UNCHANGED' | 'CONFLICT' | 'INVALID';

/** Miroir de ArticleImportRowRDto : résultat d'une ligne du fichier (`row` : numéro après l'en-tête, dès 1). */
export interface ArticleImportRowRDto {
  row: number;
  status: ImportRowStatus;
  code: string | null;
  message: string | null;
}

/** Miroir de ArticleImportRDto : compte rendu d'un import de fichier CSV. */
export interface ArticleImportRDto {
  created: number;
  updated: number;
  unchanged: number;
  conflicts: number;
  invalid: number;
  rows: ArticleImportRowRDto[];
}

/** Miroir de ArticleBatchRole (ART-06) : l'article est le produit obtenu (OUTPUT) ou un entrant consommé (INPUT). */
export type ArticleBatchRole = 'OUTPUT' | 'INPUT';

/** Miroir de ArticleBatchRDto : un lot de production vu depuis un article (quantité de cet article). */
export interface ArticleBatchRDto {
  readonly id: string;
  readonly number: string;
  readonly status: ProductionBatchStatus;
  readonly role: ArticleBatchRole;
  readonly quantity: number;
  readonly outputArticleId: string;
  readonly outputArticleCode: string;
  readonly outputArticleName: string;
  readonly createdAt: string;
  readonly completedAt: string | null;
}

/**
 * Miroir de ArticleOverviewRDto (ART-06) : fiche 360° lue en un seul appel. Lots et mouvements : les 10 plus
 * récents ; promotions : celles actives aujourd'hui qui visent l'article (catalogue, gamme ou article).
 */
export interface ArticleOverviewRDto {
  readonly article: ArticleRDto;
  readonly stock: StockRDto;
  readonly activePromotions: readonly PromotionRDto[];
  readonly productionBatches: readonly ArticleBatchRDto[];
  readonly recentMovements: readonly StockMovementRDto[];
}
