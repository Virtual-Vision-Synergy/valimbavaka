import { DEMO_ARTICLES } from '../../../shared/data-access/demo-catalog';
import { PromotionContext, PromotionScope, PromotionScopeRDto } from './promotion-models';

const finished = DEMO_ARTICLES.filter((article) => article.type === 'FINISHED');

const SCOPES: readonly PromotionScopeRDto[] = (
  ['ALL', 'BOTANIKA', 'ZEZIKA', 'SOLIKA'] as const satisfies readonly PromotionScope[]
).map((value) => ({
  value,
  articleCount:
    value === 'ALL'
      ? finished.length
      : finished.filter((article) => article.range === value).length,
}));

/**
 * Choix du périmètre du formulaire : les nombres d'articles viennent encore du catalogue de démonstration.
 * La liste des promotions, elle, vient de `GET /api/v1/promotions` (PRO-04).
 */
export const PROMOTION_SCOPES_FIXTURE: Pick<PromotionContext, 'scopes'> = { scopes: SCOPES };
