import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { SALES_DAY_FIXTURE, invoiceFixture } from './sales-fixtures';
import { InvoiceRDto, SalesDayRDto } from './sales-models';

/**
 * Ventes et factures. Provisoire : données de démonstration ; à terme `GET /api/v1/sales?date=`
 * (paginé) et `GET /api/v1/invoices/{number}`, sans changer l'appelant.
 */
@Injectable({ providedIn: 'root' })
export class SalesService {
  day(): Observable<SalesDayRDto> {
    return demo(SALES_DAY_FIXTURE);
  }

  invoice(saleNumber: string): Observable<InvoiceRDto> {
    const invoice = invoiceFixture(saleNumber);
    return invoice ? demo(invoice) : throwError(() => new Error(`Vente inconnue : ${saleNumber}`));
  }
}
