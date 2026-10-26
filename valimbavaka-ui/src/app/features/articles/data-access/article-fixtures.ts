import {
  DEMO_ARTICLES,
  DemoArticle,
  STOCK_LOCATIONS,
  totalStock,
} from '../../../shared/data-access/demo-catalog';
import { ArticleDetailRDto, ArticleRDto, StockStatus } from './article-models';

function statusOf(total: number, threshold: number): StockStatus {
  if (total === 0) {
    return 'OUT';
  }
  return total < threshold ? 'LOW' : 'AVAILABLE';
}

function toRow(article: DemoArticle): ArticleRDto {
  const total = totalStock(article);
  return {
    id: article.id,
    reference: article.reference,
    barcode: article.barcode,
    name: article.name,
    type: article.type,
    range: article.range,
    price: article.price,
    cost: article.cost,
    totalStock: total,
    threshold: article.threshold,
    status: statusOf(total, article.threshold),
  };
}

export const ARTICLES_FIXTURE: readonly ArticleRDto[] = DEMO_ARTICLES.map(toRow);

const PROMOTED = ['ART-0031', 'ART-0087'];

const ROSE_PROMOTION = {
  name: 'Octobre rose',
  rule: '3 achetés = −15 %',
  period: 'Du 1er au 31 oct.',
  code: 'ROSE15',
  uses: 38,
  rate: 15,
};

const DEMO_MOVEMENTS = [
  { when: '10:48', label: 'Vente VTE-2451 · Boutique', delta: -1 },
  { when: '09:12', label: 'Production PRD-0077 validée', delta: 40 },
  { when: '07:42', label: 'Transfert TRF-0092 · Entrepôt → Boutique', delta: -24 },
  { when: 'Hier', label: 'Retour de stock RET-0014', delta: 3 },
  { when: 'Hier', label: 'Shooting SHO-0039 · sortie', delta: -2 },
];

/** Lots de production récents ; le coût unitaire varie légèrement autour du coût de revient. */
function demoLots(cost: number): ArticleDetailRDto['lots'] {
  return [
    {
      number: 'PRD-0077',
      quantity: 40,
      operator: 'Soa R.',
      unitCost: Math.round(cost * 0.99),
      curingDaysLeft: null,
      validated: true,
    },
    {
      number: 'PRD-0071',
      quantity: 60,
      operator: 'Soa R.',
      unitCost: Math.round(cost * 1.01),
      curingDaysLeft: null,
      validated: true,
    },
    {
      number: 'PRD-0080',
      quantity: 50,
      operator: 'Fara R.',
      unitCost: null,
      curingDaysLeft: 12,
      validated: false,
    },
  ];
}

/** Fiche 360° de démonstration : chiffres de la maquette ART-06, extrapolés aux autres articles. */
export function articleDetailFixture(id: string): ArticleDetailRDto | null {
  const article = DEMO_ARTICLES.find((item) => item.id === id);
  if (!article) {
    return null;
  }
  const row = toRow(article);
  const finished = article.type === 'FINISHED';
  return {
    ...row,
    description: finished
      ? 'Saponifié à froid dans notre atelier, à partir d’huiles végétales de Madagascar.'
      : 'Intrant de l’atelier, consommé par les lots de production.',
    manufactured: finished && !article.box,
    soldLast30Days: finished ? Math.round(row.totalStock * 1.39) : 0,
    stockByLocation: STOCK_LOCATIONS.map((location) => ({
      location,
      quantity: article.stock[location],
    })),
    lots: finished ? demoLots(article.cost) : [],
    promotion: PROMOTED.includes(article.reference) ? ROSE_PROMOTION : null,
    endedPromotions: finished ? 1 : 0,
    movements: DEMO_MOVEMENTS,
  };
}
