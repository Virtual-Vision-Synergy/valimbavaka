import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { InvoiceStatusTotalsQuery, InvoiceStatusTotalsRDto } from './invoice-status-models';

/**
 * Suivi des statuts de facture (FAC-05) : `GET /api/v1/invoices/status-totals` (permission `reports.read`,
 * administrateur). Nombre et montant des factures payées, annulées et contre-passées sur une période.
 */
@Injectable({ providedIn: 'root' })
export class InvoiceStatusService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/v1/invoices/status-totals';

  get(query: InvoiceStatusTotalsQuery): Observable<InvoiceStatusTotalsRDto> {
    let params = new HttpParams();
    for (const key of ['from', 'to'] as const) {
      const value = query[key];
      if (value) {
        params = params.set(key, value);
      }
    }
    return this.http.get<InvoiceStatusTotalsRDto>(this.url, { params });
  }
}
