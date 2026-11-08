import { Tone } from '../../../shared/models/tone';
import { DeliveryOrderStatus, JournalAction } from './delivery-models';

export const DELIVERY_STATUS_TONES: Readonly<Record<DeliveryOrderStatus, Tone>> = {
  TO_PREPARE: 'muted',
  ON_ROUND: 'info',
  DELIVERED: 'success',
  FAILED: 'danger',
  CANCELLED: 'muted',
};

/** Couleur d'une étape de l'historique (LIV-06) selon l'action du journal. */
export const DELIVERY_EVENT_TONES: Readonly<Record<JournalAction, Tone>> = {
  CREATE: 'muted',
  UPDATE: 'info',
  DELETE: 'danger',
  CANCEL: 'danger',
  REVERSE: 'secondary',
};
