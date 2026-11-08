import { Tone } from '../../../shared/models/tone';
import { TransferStatus } from './transfer-models';

export const TRANSFER_STATUS_TONES: Readonly<Record<TransferStatus, Tone>> = {
  RETURN: 'muted',
  VALIDATED: 'success',
};
