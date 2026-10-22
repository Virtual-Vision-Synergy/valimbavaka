import { DEMO_ARTICLES, DemoArticle, demoArticle } from '../../../shared/data-access/demo-catalog';
import {
  ClosingLineRDto,
  ClosingLocationRDto,
  DayClosingRDto,
  DayOpeningRDto,
  DiscrepancyReason,
  DispatchLineRDto,
  DispatchQuantities,
} from './day-models';

function isoDay(offset: number): string {
  const day = new Date();
  day.setDate(day.getDate() + offset);
  return `${day.getFullYear()}-${String(day.getMonth() + 1).padStart(2, '0')}-${String(day.getDate()).padStart(2, '0')}`;
}

const CARRY_OVER: Readonly<Record<string, DispatchLineRDto['carryOver']>> = {
  'ART-0031': { destination: 'SHOP', quantity: 6 },
  'ART-0087': { destination: 'DELIVERY', quantity: 2 },
  'ART-0190': { destination: 'SALON', quantity: 1 },
};

/** Écart volontaire de la maquette : la veille, on avait demandé plus que l'entrepôt n'en a. */
const OVER_ASKED: Readonly<Record<string, DispatchQuantities>> = {
  'ART-0112': { SHOP: 10, DELIVERY: 4, SALON: 0 },
};

/** Proposition : besoin estimé (seuil), ramené à ce que l'entrepôt peut fournir. */
function propose(article: DemoArticle, index: number): DispatchQuantities {
  const wanted = {
    SHOP: Math.round(article.threshold * 0.4),
    DELIVERY: Math.round(article.threshold * 0.15),
    SALON: index % 3,
  };
  const total = wanted.SHOP + wanted.DELIVERY + wanted.SALON;
  const ratio = total > article.stock.WAREHOUSE ? article.stock.WAREHOUSE / total : 1;
  return {
    SHOP: Math.floor(wanted.SHOP * ratio),
    DELIVERY: Math.floor(wanted.DELIVERY * ratio),
    SALON: Math.floor(wanted.SALON * ratio),
  };
}

function toDispatchLine(article: DemoArticle, index: number): DispatchLineRDto {
  const proposed = OVER_ASKED[article.reference] ?? propose(article, index);
  return {
    articleId: article.id,
    reference: article.reference,
    name: article.name,
    threshold: article.threshold,
    warehouse: article.stock.WAREHOUSE,
    carryOver: CARRY_OVER[article.reference] ?? null,
    proposed,
    yesterday: { SHOP: proposed.SHOP + (index % 2), DELIVERY: proposed.DELIVERY, SALON: 0 },
  };
}

export function openingFixture(): DayOpeningRDto {
  return {
    date: isoDay(0),
    previousDay: isoDay(-1),
    rounds: 2,
    lines: DEMO_ARTICLES.filter((article) => article.type === 'FINISHED' && !article.box).map(
      toDispatchLine,
    ),
  };
}

/** [référence, transféré, retour, vendu, livré, salon, shooting, compté, motif]. */
type ClosingRow = readonly [
  string,
  number,
  number,
  number,
  number,
  number,
  number,
  number,
  DiscrepancyReason | null,
];

const SHOP_ROWS: readonly ClosingRow[] = [
  ['ART-0031', 30, 1, 19, 0, 0, 0, 12, null],
  ['ART-0087', 15, 0, 9, 0, 0, 1, 4, 'BREAKAGE'],
  ['ART-0201', 20, 0, 11, 0, 0, 1, 8, null],
  ['ART-0154', 12, 0, 5, 0, 0, 0, 7, null],
  ['ART-0133', 8, 0, 4, 0, 0, 2, 2, null],
  ['ART-0190', 6, 0, 2, 0, 1, 0, 2, null],
  ['ART-0176', 10, 0, 6, 0, 0, 0, 4, null],
  ['ART-0045', 12, 0, 7, 0, 0, 0, 5, null],
];

const DELIVERY_ROWS: readonly ClosingRow[] = [
  ['ART-0031', 8, 1, 0, 6, 0, 0, 3, null],
  ['ART-0112', 4, 0, 0, 2, 0, 0, 2, null],
  ['ART-0087', 4, 0, 0, 3, 0, 0, 0, 'ENTRY_ERROR'],
  ['ART-0154', 4, 1, 0, 3, 0, 0, 2, null],
  ['ART-0133', 4, 0, 0, 2, 0, 0, 2, null],
];

const SALON_ROWS: readonly ClosingRow[] = [
  ['ART-0190', 4, 0, 0, 0, 3, 0, 1, null],
  ['ART-0112', 0, 0, 0, 0, 0, 0, 0, null],
  ['ART-0201', 2, 0, 1, 0, 1, 0, 0, null],
];

function toClosingLine(row: ClosingRow): ClosingLineRDto {
  const [reference, transferred, returned, sold, delivered, salon, shooting, counted, reason] = row;
  const article = demoArticle(reference);
  return {
    articleId: article.id,
    reference,
    name: article.name,
    transferred,
    returned,
    sold,
    delivered,
    salon,
    shooting,
    counted,
    reason,
  };
}

const CLOSING_LOCATIONS: readonly ClosingLocationRDto[] = [
  {
    location: 'SHOP',
    lines: SHOP_ROWS.map(toClosingLine),
    cash: { expected: 486500, counted: 485000, otherMethods: 798500 },
  },
  {
    location: 'DELIVERY',
    lines: DELIVERY_ROWS.map(toClosingLine),
    cash: { expected: 196000, counted: 196000, otherMethods: 53000 },
  },
  {
    location: 'SALON',
    lines: SALON_ROWS.map(toClosingLine),
    cash: { expected: 64000, counted: 64000, otherMethods: 25000 },
  },
];

export function closingFixture(): DayClosingRDto {
  return {
    date: isoDay(0),
    locations: CLOSING_LOCATIONS,
    checklist: { deliveriesDone: 14, deliveriesTotal: 14, heldTickets: 0, lateShootings: 1 },
  };
}
