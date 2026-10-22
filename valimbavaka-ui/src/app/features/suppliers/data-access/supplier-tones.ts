import { Tone } from '../../../shared/models/tone';
import { SupplierType } from './supplier-models';

export const SUPPLIER_TYPE_TONES: Readonly<Record<SupplierType, Tone>> = {
  RAW_MATERIAL: 'secondary',
  PACKAGING: 'info',
  FINISHED: 'warning',
  IMPORT: 'muted',
};

/** Délai au-delà duquel le réapprovisionnement doit être anticipé (mention « long »). */
export const LONG_LEAD_TIME_DAYS = 12;
