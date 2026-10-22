import { Tone } from '../../../shared/models/tone';
import { LotStatus } from './production-models';

export const LOT_STATUS_TONES: Readonly<Record<LotStatus, Tone>> = {
  DRAFT: 'muted',
  CURING: 'info',
  VALIDATED: 'success',
};
