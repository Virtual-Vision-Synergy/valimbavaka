import {
  ProductRangeRDto,
  SalesChannelRDto,
} from '../../referential/data-access/referential-models';
import { SaleChannel } from '../../../shared/models/commerce';
import { PageQuery } from '../../../shared/models/page-response';

export type PromotionKind = 'PERCENT' | 'AMOUNT' | 'BUY_X';
export type PromotionStatus = 'ACTIVE' | 'SCHEDULED' | 'ENDED';
/** Périmètre côté serveur : tout le catalogue, des gammes ou des articles (`targetIds`). */
export type PromotionTargetScope = 'ALL' | 'RANGES' | 'ARTICLES';

export const PROMOTION_KINDS: readonly PromotionKind[] = ['PERCENT', 'AMOUNT', 'BUY_X'];
export const PROMOTION_STATUSES: readonly PromotionStatus[] = ['ACTIVE', 'SCHEDULED', 'ENDED'];

/** Choix du formulaire : tout le catalogue ou une gamme (code du référentiel). */
export type PromotionScope = 'ALL' | 'BOTANIKA' | 'ZEZIKA' | 'SOLIKA';

export interface PromotionScopeRDto {
  readonly value: PromotionScope;
  readonly articleCount: number;
}

/** Carte de promotion affichée dans la liste (construite depuis `PromotionRDto` par `toCardView`). */
export interface PromotionCardView {
  readonly id: string;
  readonly name: string;
  readonly description: string;
  readonly kind: PromotionKind;
  readonly value: number;
  readonly code: string | null;
  readonly startsOn: string;
  readonly endsOn: string;
  readonly uses: number;
  readonly limit: number | null;
  readonly discountGranted: number;
}

/** Référentiels utiles à la création : gammes ciblables et canaux de vente (SYS-05). */
export interface PromotionReferences {
  readonly ranges: readonly ProductRangeRDto[];
  readonly channels: readonly SalesChannelRDto[];
}

/** Contexte du formulaire : référentiels réels et choix du périmètre (nombres d'articles de démonstration). */
export interface PromotionContext extends PromotionReferences {
  readonly scopes: readonly PromotionScopeRDto[];
}

/** Paramètres de `GET /api/v1/promotions` : sans statut, toutes les promotions. */
export interface PromotionListQuery extends PageQuery {
  status?: PromotionStatus;
}

/** Saisie du formulaire, avant traduction en identifiants du référentiel. */
export interface PromotionDraft {
  readonly name: string;
  readonly kind: PromotionKind;
  readonly value: number;
  readonly code: string | null;
  readonly scope: PromotionScope;
  readonly startsOn: string;
  readonly endsOn: string;
  readonly channels: readonly SaleChannel[];
  readonly limit: number | null;
}

/** Miroir de `PromotionWDto` (POST /api/v1/promotions) ; le statut est déduit des dates par le serveur. */
export interface PromotionWDto {
  readonly name: string;
  readonly description: string | null;
  readonly kind: PromotionKind;
  readonly value: number;
  readonly code: string | null;
  readonly scope: PromotionTargetScope;
  readonly targetIds: readonly string[];
  readonly startsOn: string;
  readonly endsOn: string;
  readonly channelIds: readonly string[];
  readonly usageLimit: number | null;
}

/** Miroir de `PromotionRDto`. */
export interface PromotionRDto {
  readonly id: string;
  readonly name: string;
  readonly description: string | null;
  readonly kind: PromotionKind;
  readonly value: number;
  readonly code: string | null;
  readonly scope: PromotionTargetScope;
  readonly targetIds: readonly string[];
  readonly startsOn: string;
  readonly endsOn: string;
  readonly channelIds: readonly string[];
  readonly usageLimit: number | null;
  readonly usageCount: number;
  readonly status: PromotionStatus;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly version: number;
}
