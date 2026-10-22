import { DEMO_ARTICLES } from '../../../shared/data-access/demo-catalog';
import { isoDay } from '../../../shared/data-access/demo-dates';
import {
  PromotionContextRDto,
  PromotionKind,
  PromotionRDto,
  PromotionScope,
  PromotionScopeRDto,
} from './promotion-models';

/** [nom, description, type, valeur, code, début (j), fin (j), utilisations, plafond, remise]. */
type PromotionRow = readonly [
  string,
  string,
  PromotionKind,
  number,
  string | null,
  number,
  number,
  number,
  number | null,
  number,
];

const ROWS: readonly PromotionRow[] = [
  [
    'Octobre rose',
    'Dès 3 articles Botanika ou Zezika',
    'PERCENT',
    15,
    'ROSE15',
    -20,
    10,
    38,
    null,
    186400,
  ],
  [
    'Livraison offerte',
    'Commandes Facebook > 50 000 Ar',
    'AMOUNT',
    0,
    'LIVOK',
    -6,
    24,
    12,
    100,
    36000,
  ],
  ['Black Friday', 'Tout le catalogue', 'PERCENT', 25, 'BF25', 37, 39, 0, 300, 0],
  ['Rentrée', 'Gamme Solika', 'PERCENT', 10, 'RENTREE', -52, -22, 64, 100, 142800],
  ['2 + 1 savons', 'Savons 100 g', 'BUY_X', 3, null, -83, -52, 41, null, 348500],
  ['Fête de l’indépendance', 'Coffrets', 'PERCENT', 26, 'MADA26', -124, -118, 26, 26, 236600],
  ['Fête des mères', 'Gamme Zezika', 'PERCENT', 20, 'MAMAN20', -145, -138, 52, 200, 198000],
  ['Soldes d’hiver', 'Fins de série', 'PERCENT', 30, 'HIVER30', -100, -86, 33, 80, 154300],
  ['Pack découverte', 'Coffret Botanika', 'AMOUNT', 5000, 'DECOUV', -170, -140, 18, 50, 90000],
];

function toPromotion(row: PromotionRow, index: number): PromotionRDto {
  const [name, description, kind, value, code, from, to, uses, limit, discountGranted] = row;
  return {
    id: `pro-${String(index + 1).padStart(2, '0')}`,
    name,
    description,
    kind,
    value,
    code,
    startsOn: isoDay(from),
    endsOn: isoDay(to),
    uses,
    limit,
    discountGranted,
  };
}

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

export const PROMOTIONS_FIXTURE: PromotionContextRDto = {
  promotions: ROWS.map(toPromotion),
  scopes: SCOPES,
};
