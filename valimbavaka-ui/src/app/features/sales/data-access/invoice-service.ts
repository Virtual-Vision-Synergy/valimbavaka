import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { InvoiceRDto } from './sales-models';

/**
 * Factures émises (FAC-01) : `GET /api/v1/invoices/sales/{saleId}` (permission `sales.read`). Une facture est créée
 * par le serveur à l'enregistrement de la vente, jamais par l'interface. Une vendeuse ne lit que les factures de ses
 * ventes (404 sinon) ; une vente antérieure à la facturation automatique n'en a pas (404).
 *
 * Régénération (FAC-02) : `POST /api/v1/invoices/sales/{saleId}/regenerate` (permission `invoices.regenerate`), sans
 * corps : le serveur rend la même facture (même numéro, même contenu) ou, pour une vente sans facture, l'émet.
 */
@Injectable({ providedIn: 'root' })
export class InvoiceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/invoices';

  getBySale(saleId: string): Observable<InvoiceRDto> {
    return this.http.get<InvoiceRDto>(`${this.baseUrl}/sales/${encodeURIComponent(saleId)}`);
  }

  regenerate(saleId: string): Observable<InvoiceRDto> {
    return this.http.post<InvoiceRDto>(
      `${this.baseUrl}/sales/${encodeURIComponent(saleId)}/regenerate`,
      null,
    );
  }

  getById(invoiceId: string): Observable<InvoiceRDto> {
    return this.http.get<InvoiceRDto>(`${this.baseUrl}/${encodeURIComponent(invoiceId)}`);
  }
}
