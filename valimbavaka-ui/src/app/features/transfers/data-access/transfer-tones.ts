import { Tone } from '../../../shared/models/tone';
import { TransferRowStatus } from './transfer-models';

export const TRANSFER_STATUS_TONES: Readonly<Record<TransferRowStatus, Tone>> = {
  IN_TRANSIT: 'info',
  RECEIVED: 'success',
  DISCREPANCY: 'warning',
  RETURN: 'muted',
};
