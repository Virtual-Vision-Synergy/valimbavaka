import { InvoiceLineRDto } from './sales-models';

/**
 * Avoir motivé d'une facture (FAC-04, miroir de `CreditNoteRDto`) : copie figée de la facture contre-passée, avec son
 * propre numéro (`AV-2026-0007`) et le motif saisi. Jamais modifié ni supprimé.
 */
export interface CreditNoteRDto {
  readonly id: string;
  readonly number: string;
  readonly invoiceId: string;
  readonly invoiceNumber: string;
  readonly saleId: string;
  readonly saleNumber: string;
  readonly issuedAt: string;
  readonly businessDate: string;
  readonly reason: string;
  readonly issuerName: string;
  readonly issuerNif: string | null;
  readonly subtotal: number;
  readonly discountAmount: number;
  readonly total: number;
  readonly lines: readonly InvoiceLineRDto[];
}

/** Corps de l'émission d'un avoir (miroir de `CreditNoteWDto`) : le motif seul, le serveur reprend la facture. */
export interface CreditNoteWDto {
  readonly reason: string;
}

/** Longueur maximale du motif (miroir de `@Size(max = 500)` côté serveur). */
export const CREDIT_NOTE_REASON_MAX = 500;
