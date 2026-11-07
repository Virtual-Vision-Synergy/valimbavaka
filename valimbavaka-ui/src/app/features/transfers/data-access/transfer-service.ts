import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { demo } from '../../../shared/data-access/demo';
import { TRANSFERS_FIXTURE, transferDetailFixture } from './transfer-fixtures';
import {
  StockReturnWDto,
  TransferDetailRDto,
  TransferRDto,
  TransferRowRDto,
} from './transfer-models';

/**
 * Transferts de stock. Le retour de stock (TRF-03) passe par la file hors ligne (`OutboxService`, SYS-07) ;
 * la liste et le détail restent des données de démonstration jusqu'à TRF-01 (`GET /api/v1/transfers`, paginé)
 * et TRF-04 (`GET /api/v1/transfers/{id}`).
 */
@Injectable({ providedIn: 'root' })
export class TransferService {
  private readonly outbox = inject(OutboxService);
  private readonly baseUrl = '/api/v1/transfers';

  list(): Observable<readonly TransferRowRDto[]> {
    return demo(TRANSFERS_FIXTURE);
  }

  get(id: string): Observable<TransferDetailRDto | null> {
    return demo(transferDetailFixture(id));
  }

  /** Enregistre un retour (statut `RETURN`, numéro `RET-…`) ; `queued` vaut vrai si la saisie attend le réseau. */
  recordReturn(body: StockReturnWDto): Observable<SubmitResult<TransferRDto>> {
    return this.outbox.submit<TransferRDto>('POST', `${this.baseUrl}/returns`, body);
  }
}
