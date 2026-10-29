import { STOCK_LOCATIONS, StockLocation } from '../../../shared/data-access/demo-catalog';
import { ProductRange } from '../../../shared/models/tone';
import { ProductRangeRDto } from '../../referential/data-access/referential-models';
import { StockRDto, StockRowRDto } from './stock-models';

/** Codes d'emplacement du serveur (référentiel SYS-05) → codes utilisés par l'interface. */
const LOCATION_BY_CODE: Readonly<Record<string, StockLocation>> = {
  WAREHOUSE: 'WAREHOUSE',
  STORE: 'SHOP',
  DELIVERY: 'DELIVERY',
  TRADE_SHOW: 'SALON',
};

/** Colonnes que l'API ne fournit pas encore (STK-05 : fournisseur principal, délai, ventes journalières). */
const PENDING_RESTOCK_FIELDS = { supplierName: null, leadTimeDays: null, dailySales: 0 } as const;

export function toStockRow(stock: StockRDto, ranges: readonly ProductRangeRDto[]): StockRowRDto {
  return {
    articleId: stock.articleId,
    reference: stock.code,
    name: stock.name,
    range: rangeOf(stock.rangeId, ranges),
    quantities: quantitiesOf(stock),
    threshold: stock.threshold,
    unitCost: stock.cost ?? 0,
    ...PENDING_RESTOCK_FIELDS,
  };
}

function rangeOf(rangeId: string | null, ranges: readonly ProductRangeRDto[]): ProductRange | null {
  const code = ranges.find((range) => range.id === rangeId)?.code;
  return code === 'BOTANIKA' || code === 'ZEZIKA' || code === 'SOLIKA' ? code : null;
}

function quantitiesOf(stock: StockRDto): Record<StockLocation, number> {
  const quantities = Object.fromEntries(STOCK_LOCATIONS.map((location) => [location, 0])) as Record<
    StockLocation,
    number
  >;
  for (const item of stock.locations) {
    const location = LOCATION_BY_CODE[item.code];
    if (location) {
      quantities[location] += item.quantity;
    }
  }
  return quantities;
}
