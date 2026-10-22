import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { STOCK_FIXTURE } from './stock-fixtures';
import { StockRowRDto } from './stock-models';

/**
 * Stocks par emplacement. Provisoire : données de démonstration ; à terme `GET /api/v1/stocks`
 * (projection du journal, jamais modifiable à la main), sans changer l'appelant.
 */
@Injectable({ providedIn: 'root' })
export class StockService {
  list(): Observable<readonly StockRowRDto[]> {
    return demo(STOCK_FIXTURE);
  }
}
