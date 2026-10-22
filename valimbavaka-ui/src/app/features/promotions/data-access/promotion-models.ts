import { SaleChannel } from '../../../shared/models/commerce';

export type PromotionKind = 'PERCENT' | 'AMOUNT' | 'BUY_X';
export type PromotionStatus = 'ACTIVE' | 'SCHEDULED' | 'ENDED';

export const PROMOTION_KINDS: readonly PromotionKind[] = ['PERCENT', 'AMOUNT', 'BUY_X'];
export const PROMOTION_STATUSES: readonly PromotionStatus[] = ['ACTIVE', 'SCHEDULED', 'ENDED'];

/** Périmètre d'une promotion : tout le catalogue ou une gamme. */
export type PromotionScope = 'ALL' | 'BOTANIKA' | 'ZEZIKA' | 'SOLIKA';

export interface PromotionScopeRDto {
  readonly value: PromotionScope;
  readonly articleCount: number;
}

/** Carte de promotion (PRO-01, PRO-03). */
export interface PromotionRDto {
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

export interface PromotionContextRDto {
  readonly promotions: readonly PromotionRDto[];
  readonly scopes: readonly PromotionScopeRDto[];
}

/** Création d'une promotion (PRO-02) ; le statut est déduit des dates par le serveur. */
export interface PromotionWDto {
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
