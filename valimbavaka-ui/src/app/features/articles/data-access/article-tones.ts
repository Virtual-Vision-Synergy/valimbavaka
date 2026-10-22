import { Tone } from '../../../shared/models/tone';
import { StockStatus } from './article-models';

/** Tonalité du badge de disponibilité (toujours doublée du libellé). */
export const STOCK_STATUS_TONES: Readonly<Record<StockStatus, Tone>> = {
  AVAILABLE: 'success',
  LOW: 'warning',
  OUT: 'danger',
};
