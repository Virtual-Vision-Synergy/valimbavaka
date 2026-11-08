import { Tone } from '../../../shared/models/tone';
import { ProductionBatchStatus } from '../../production/data-access/production-models';
import { StockStatus } from './article-models';

/** Tonalité du badge de disponibilité (toujours doublée du libellé). */
export const STOCK_STATUS_TONES: Readonly<Record<StockStatus, Tone>> = {
  AVAILABLE: 'success',
  LOW: 'warning',
  OUT: 'danger',
};

/** Tonalité du statut d'un lot de production sur la fiche 360° (ART-06). */
export const BATCH_STATUS_TONES: Readonly<Record<ProductionBatchStatus, Tone>> = {
  PLANNED: 'info',
  IN_PROGRESS: 'warning',
  COMPLETED: 'success',
};
