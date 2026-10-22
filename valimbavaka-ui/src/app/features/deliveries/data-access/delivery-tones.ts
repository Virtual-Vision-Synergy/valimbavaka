import { Tone } from '../../../shared/models/tone';
import { DeliveryEventKind, DeliveryStatus } from './delivery-models';

export const DELIVERY_STATUS_TONES: Readonly<Record<DeliveryStatus, Tone>> = {
  TO_PREPARE: 'muted',
  ON_ROUND: 'info',
  DELIVERED: 'success',
  FAILED: 'danger',
};

export const DELIVERY_EVENT_TONES: Readonly<Record<DeliveryEventKind, Tone>> = {
  CREATED: 'muted',
  PREPARED: 'secondary',
  ON_ROUND: 'info',
  ARRIVED: 'info',
  DELIVERED: 'success',
  FAILED: 'danger',
};
