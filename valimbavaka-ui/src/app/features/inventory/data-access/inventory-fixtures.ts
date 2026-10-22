import { demoArticle } from '../../../shared/data-access/demo-catalog';
import { isoDay } from '../../../shared/data-access/demo-dates';
import {
  InventoryContextRDto,
  InventoryLineRDto,
  InventoryReason,
  PreviousSessionRDto,
} from './inventory-models';

/** [référence, théorique en boutique, compté (null = à compter), motif]. */
type CountRow = readonly [string, number, number | null, InventoryReason | null];

const ROWS: readonly CountRow[] = [
  ['ART-0031', 42, 42, null],
  ['ART-0087', 3, 1, 'BREAKAGE'],
  ['ART-0201', 14, 12, null],
  ['ART-0133', 9, 9, null],
  ['ART-0154', 12, 13, 'ENTRY_ERROR'],
  ['ART-0190', 11, 9, 'UNDECLARED_SHOOTING'],
  ['ART-0176', 12, 12, null],
  ['ART-0118', 7, null, null],
  ['ART-0045', 21, null, null],
  ['ART-0112', 4, 4, null],
  ['ART-0240', 6, 6, null],
  ['ART-0162', 15, null, null],
];

function toLine([reference, theoretical, counted, reason]: CountRow): InventoryLineRDto {
  const article = demoArticle(reference);
  return {
    articleId: article.id,
    reference,
    barcode: article.barcode,
    name: article.name,
    theoretical,
    unitCost: article.cost,
    counted,
    reason,
  };
}

const PREVIOUS: readonly PreviousSessionRDto[] = [
  { number: 'INV-0011', location: 'WAREHOUSE', closedOn: isoDay(-22), gapUnits: -12 },
  { number: 'INV-0010', location: 'SALON', closedOn: isoDay(-24), gapUnits: 0 },
  { number: 'INV-0009', location: 'SHOP', closedOn: isoDay(-52), gapUnits: -3 },
  { number: 'INV-0008', location: 'DELIVERY', closedOn: isoDay(-52), gapUnits: 1 },
];

export const INVENTORY_FIXTURE: InventoryContextRDto = {
  session: {
    number: 'INV-0012',
    location: 'SHOP',
    openedAt: `${isoDay(0)}T18:30`,
    openedBy: 'Hanta R.',
    status: 'IN_PROGRESS',
    lines: ROWS.map(toLine),
  },
  previous: PREVIOUS,
};
