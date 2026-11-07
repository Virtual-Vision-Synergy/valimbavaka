import { Tone } from '../../../shared/models/tone';
import { TransferRowStatus, TransferStatus } from './transfer-models';

export const TRANSFER_STATUS_TONES: Readonly<Record<TransferRowStatus | TransferStatus, Tone>> = {
  IN_TRANSIT: 'info',
  RECEIVED: 'success',
  DISCREPANCY: 'warning',
  RETURN: 'muted',
  VALIDATED: 'success',
};
