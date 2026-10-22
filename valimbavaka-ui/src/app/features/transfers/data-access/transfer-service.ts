import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { TRANSFERS_FIXTURE, transferDetailFixture } from './transfer-fixtures';
import { TransferDetailRDto, TransferRDto } from './transfer-models';

/**
 * Transferts de stock. Provisoire : données de démonstration ; à terme `GET /api/v1/transfers`
 * (paginé) et `GET /api/v1/transfers/{id}`.
 */
@Injectable({ providedIn: 'root' })
export class TransferService {
  list(): Observable<readonly TransferRDto[]> {
    return demo(TRANSFERS_FIXTURE);
  }

  get(id: string): Observable<TransferDetailRDto | null> {
    return demo(transferDetailFixture(id));
  }
}
