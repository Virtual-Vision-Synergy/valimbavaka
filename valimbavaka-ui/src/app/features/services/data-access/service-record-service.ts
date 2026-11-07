import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageResponse } from '../../../shared/models/page-response';
import {
  ServiceRecordDayQuery,
  ServiceRecordListQuery,
  ServiceRecordRDto,
  ServiceRecordWDto,
} from './service-record-models';

/**
 * Prestations salon (SRV-02) : l'enregistrement déduit les produits consommés du stock salon côté serveur. Lecture
 * avec `services.read` (dont l'historique du jour du compte connecté, SRV-03), enregistrement avec
 * `service-records.create` ; pas de modification ni de suppression.
 * L'écriture passe par la file hors ligne (`OutboxService`, SYS-07) : `queued` vaut vrai si elle attend le réseau.
 */
@Injectable({ providedIn: 'root' })
export class ServiceRecordService {
  private readonly http = inject(HttpClient);
  private readonly outbox = inject(OutboxService);
  private readonly baseUrl = '/api/v1/service-records';

  list(query: ServiceRecordListQuery): Observable<PageResponse<ServiceRecordRDto>> {
    return this.http.get<PageResponse<ServiceRecordRDto>>(this.baseUrl, {
      params: toHttpParams(query),
    });
  }

  /** Prestations du compte connecté sur un jour (aujourd'hui par défaut), les plus récentes d'abord (SRV-03). */
  mine(query: ServiceRecordDayQuery): Observable<PageResponse<ServiceRecordRDto>> {
    let params = toHttpParams(query);
    if (query.date) {
      params = params.set('date', query.date);
    }
    return this.http.get<PageResponse<ServiceRecordRDto>>(`${this.baseUrl}/mine`, { params });
  }

  getById(id: string): Observable<ServiceRecordRDto> {
    return this.http.get<ServiceRecordRDto>(`${this.baseUrl}/${id}`);
  }

  create(body: ServiceRecordWDto): Observable<SubmitResult<ServiceRecordRDto>> {
    return this.outbox.submit<ServiceRecordRDto>('POST', this.baseUrl, body);
  }
}
