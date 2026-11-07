import { STOCK_LOCATIONS, StockLocation } from '../../../shared/data-access/demo-catalog';
import { StockGaugeSegment } from '../../../shared/components/stock-gauge/stock-gauge';
import { StockRowRDto } from './stock-models';

export type LocationFilter = 'ALL' | StockLocation;
export type StockLevel = 'OK' | 'LOW' | 'OUT';

/** Quantité considérée selon l'emplacement choisi (tous = somme). */
export function quantityAt(row: StockRowRDto, location: LocationFilter): number {
  return location === 'ALL'
    ? STOCK_LOCATIONS.reduce((sum, item) => sum + row.quantities[item], 0)
    : row.quantities[location];
}

/** Seuil d'un emplacement : le seuil article est réparti, l'ensemble garde le seuil complet. */
export function thresholdAt(row: StockRowRDto, location: LocationFilter): number {
  return location === 'ALL' ? row.threshold : Math.ceil(row.threshold / 4);
}

export function levelAt(row: StockRowRDto, location: LocationFilter): StockLevel {
  const quantity = quantityAt(row, location);
  if (quantity === 0) {
    return 'OUT';
  }
  return quantity < thresholdAt(row, location) ? 'LOW' : 'OK';
}

/** Répartition de la jauge (STK-02) : une part par emplacement, dans l'ordre des colonnes, zéro compris. */
export function segmentsOf(row: StockRowRDto): readonly StockGaugeSegment[] {
  return STOCK_LOCATIONS.map((item) => ({
    key: item,
    labelKey: `stocks.location.${item}`,
    quantity: row.quantities[item],
  }));
}
