import { STOCK_LOCATIONS, StockLocation } from '../../../shared/data-access/demo-catalog';
import { StockRowRDto } from './stock-models';

export type LocationFilter = 'ALL' | StockLocation;
export type StockLevel = 'OK' | 'LOW' | 'OUT';

/** Jours de ventes couverts par un bon de réapprovisionnement (STK-05). */
export const COVERAGE_DAYS = 30;

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

/** Quantité proposée : couvrir 30 jours de ventes au-delà du stock actuel, arrondie à la dizaine. */
export function proposedQuantity(row: StockRowRDto): number {
  const missing = row.dailySales * COVERAGE_DAYS - quantityAt(row, 'ALL');
  return Math.max(10, Math.ceil(Math.max(missing, row.threshold) / 10) * 10);
}
