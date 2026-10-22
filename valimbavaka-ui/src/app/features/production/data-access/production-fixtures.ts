import { isoDay } from '../../../shared/data-access/demo-dates';
import { demoArticle } from '../../../shared/data-access/demo-catalog';
import {
  LotRDto,
  MaterialRDto,
  ProducibleArticleRDto,
  ProductionContextRDto,
  RecipeLineRDto,
} from './production-models';

const MATERIALS: readonly MaterialRDto[] = [
  {
    id: 'mp-0008',
    reference: 'MP-0008',
    name: 'Base savonneuse',
    unit: 'kg',
    available: 12.5,
    unitCost: 12000,
  },
  {
    id: 'mp-0021',
    reference: 'MP-0021',
    name: 'Huile essentielle ravintsara',
    unit: 'ml',
    available: 45,
    unitCost: 450,
  },
  {
    id: 'mp-0017',
    reference: 'MP-0015',
    name: 'Argile verte',
    unit: 'g',
    available: 4200,
    unitCost: 15,
  },
  {
    id: 'emb-0011',
    reference: 'EMB-0011',
    name: 'Étiquette Botanika',
    unit: 'u.',
    available: 420,
    unitCost: 550,
  },
  {
    id: 'mp-0012',
    reference: 'MP-0012',
    name: 'Beurre de karité brut',
    unit: 'kg',
    available: 9,
    unitCost: 21000,
  },
  {
    id: 'mp-0030',
    reference: 'MP-0030',
    name: 'Café moulu',
    unit: 'g',
    available: 7000,
    unitCost: 12,
  },
  {
    id: 'emb-0004',
    reference: 'EMB-0004',
    name: 'Pot verre ambré 150 ml',
    unit: 'u.',
    available: 240,
    unitCost: 1200,
  },
];

/** [référence article, [matière, quantité par unité]…]. */
type RecipeRow = readonly [string, readonly (readonly [string, number])[]];

const RECIPES: readonly RecipeRow[] = [
  [
    'ART-0176',
    [
      ['mp-0008', 0.1],
      ['mp-0021', 2],
      ['mp-0017', 20],
      ['emb-0011', 1],
    ],
  ],
  [
    'ART-0031',
    [
      ['mp-0008', 0.08],
      ['mp-0017', 4],
      ['emb-0011', 1],
    ],
  ],
  [
    'ART-0087',
    [
      ['mp-0008', 0.05],
      ['mp-0030', 40],
      ['emb-0011', 1],
    ],
  ],
  [
    'ART-0154',
    [
      ['mp-0012', 0.12],
      ['emb-0004', 1],
      ['emb-0011', 1],
    ],
  ],
];

function toProduct([reference, lines]: RecipeRow): ProducibleArticleRDto {
  const article = demoArticle(reference);
  const recipe: RecipeLineRDto[] = lines.map(([materialId, perUnit]) => ({ materialId, perUnit }));
  return { articleId: article.id, reference, name: article.name, price: article.price, recipe };
}

/** [n°, jours avant aujourd'hui, référence, quantité, opérateur, statut]. */
const LOTS: readonly LotRDto[] = (
  [
    ['PRD-0077', 0, 'ART-0031', 40, 'Soa R.', 'VALIDATED'],
    ['PRD-0079', 1, 'ART-0087', 24, 'Fara R.', 'DRAFT'],
    ['PRD-0080', 3, 'ART-0031', 50, 'Fara R.', 'CURING'],
    ['PRD-0076', 5, 'ART-0201', 80, 'Soa R.', 'VALIDATED'],
    ['PRD-0074', 7, 'ART-0190', 30, 'Soa R.', 'CURING'],
    ['PRD-0071', 10, 'ART-0031', 60, 'Soa R.', 'VALIDATED'],
  ] as const
).map(([number, ago, reference, quantity, operator, status]) => ({
  number,
  date: isoDay(-ago),
  articleName: demoArticle(reference).name,
  quantity,
  operator,
  status,
}));

export const PRODUCTION_FIXTURE: ProductionContextRDto = {
  nextNumber: 'PRD-0081',
  operators: ['Soa Rakoto', 'Fara Rasoa', 'Hanta Rasolofoson'],
  materials: MATERIALS,
  products: RECIPES.map(toProduct),
  recentLots: LOTS,
  kpis: {
    lotsThisMonth: 18,
    curing: 3,
    unitsProduced: 864,
    unitsTrendPercent: 12,
    averageCost: 3410,
    materialsToRebuy: 2,
  },
};
