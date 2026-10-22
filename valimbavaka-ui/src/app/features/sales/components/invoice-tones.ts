import { Tone } from '../../../shared/models/tone';
import { InvoiceStatus } from '../data-access/sales-models';

/** Tonalité du badge de chaque statut de facture (toujours doublée du libellé). */
export const INVOICE_TONES: Readonly<Record<InvoiceStatus, Tone>> = {
  PAID: 'success',
  SENT: 'info',
  PENDING: 'warning',
  CREDITED: 'danger',
};
