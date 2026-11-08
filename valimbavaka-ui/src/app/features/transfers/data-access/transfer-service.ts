import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageResponse } from '../../../shared/models/page-response';
import {
  StockReturnWDto,
  TransferDetailRDto,
  TransferListQuery,
  TransferRDto,
  TransferSummaryRDto,
} from './transfer-models';

/**
 * Transferts de stock. L'historique (TRF-01) est lu sur `GET /api/v1/transfers` (paginé) ; le retour de stock
 * (TRF-03) passe par la file hors ligne (`OutboxService`, SYS-07) ; le détail d'un bon (TRF-04) est lu sur
 * `GET /api/v1/transfers/{id}`.
 */
@Injectable({ providedIn: 'root' })
export class TransferService {
  private readonly http = inject(HttpClient);
  private readonly outbox = inject(OutboxService);
  private readonly baseUrl = '/api/v1/transfers';

  /** Historique, du plus récent au plus ancien ; `origin` absent = toutes les origines. */
  list(query: TransferListQuery): Observable<PageResponse<TransferSummaryRDto>> {
    const params = toHttpParams(query);
    return this.http.get<PageResponse<TransferSummaryRDto>>(this.baseUrl, {
      params: query.origin ? params.set('origin', query.origin) : params,
    });
  }

  /** Détail complet d'un transfert : articles, quantités, trajet, émetteur et note (404 s'il est inconnu). */
  get(id: string): Observable<TransferDetailRDto> {
    return this.http.get<TransferDetailRDto>(`${this.baseUrl}/${encodeURIComponent(id)}`);
  }

  /** Enregistre un retour (statut `RETURN`, numéro `RET-…`) ; `queued` vaut vrai si la saisie attend le réseau. */
  recordReturn(body: StockReturnWDto): Observable<SubmitResult<TransferRDto>> {
    return this.outbox.submit<TransferRDto>('POST', `${this.baseUrl}/returns`, body);
  }
}
