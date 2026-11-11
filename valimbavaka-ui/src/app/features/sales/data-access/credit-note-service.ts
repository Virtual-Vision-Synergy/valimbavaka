import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CreditNoteRDto, CreditNoteWDto } from './credit-note-models';

/**
 * Avoirs (FAC-04) : `POST /api/v1/invoices/{invoiceId}/credit-note` (permission `invoices.credit`, administrateur) émet
 * l'avoir motivé d'une facture : le serveur annule la vente et remet les articles en stock dans la même transaction.
 * `GET` du même chemin (permission `sales.read`) lit l'avoir d'une facture (404 si elle n'en a pas). Pas de file hors
 * ligne : l'avoir est une opération légale qui exige la réponse du serveur (409 : déjà contre-passée, journée close).
 */
@Injectable({ providedIn: 'root' })
export class CreditNoteService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/invoices';

  issue(invoiceId: string, request: CreditNoteWDto): Observable<CreditNoteRDto> {
    return this.http.post<CreditNoteRDto>(this.url(invoiceId), request);
  }

  getByInvoice(invoiceId: string): Observable<CreditNoteRDto> {
    return this.http.get<CreditNoteRDto>(this.url(invoiceId));
  }

  private url(invoiceId: string): string {
    return `${this.baseUrl}/${encodeURIComponent(invoiceId)}/credit-note`;
  }
}
