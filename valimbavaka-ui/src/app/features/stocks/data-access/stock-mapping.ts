import { STOCK_LOCATIONS, StockLocation } from '../../../shared/data-access/demo-catalog';
import { ProductRange } from '../../../shared/models/tone';
import { ProductRangeRDto } from '../../referential/data-access/referential-models';
import { LocationFilter } from './stock-levels';
import { StockRDto, StockRowRDto, StockValuationRDto } from './stock-models';

/** Quantité et valeur d'une carte d'emplacement, calculées par le serveur sur tout le stock (STK-03). */
export interface LocationTotal {
  readonly units: number;
  readonly value: number;
  readonly unvaluedUnits: number;
}

export type LocationTotals = Readonly<Record<LocationFilter, LocationTotal>>;

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

/** Valorisation du serveur rangée sous les emplacements de l'interface ; « Tous » reprend les totaux. */
export function toLocationTotals(valuation: StockValuationRDto): LocationTotals {
  const totals = Object.fromEntries(
    STOCK_LOCATIONS.map((location) => [location, { units: 0, value: 0, unvaluedUnits: 0 }]),
  ) as Record<StockLocation, LocationTotal>;
  for (const item of valuation.locations) {
    const location = LOCATION_BY_CODE[item.code];
    if (location) {
      totals[location] = {
        units: item.quantity,
        value: item.value,
        unvaluedUnits: item.unvaluedQuantity,
      };
    }
  }
  return {
    ...totals,
    ALL: {
      units: valuation.quantity,
      value: valuation.value,
      unvaluedUnits: valuation.unvaluedQuantity,
    },
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
