import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageResponse } from '../../../shared/models/page-response';
import {
  SalonServiceListQuery,
  SalonServiceRDto,
  SalonServiceStatusWDto,
  SalonServiceWDto,
} from './salon-catalog-models';

/**
 * Catalogue des services salon (SRV-01) : lecture pour l'administrateur et la vendeuse, écriture pour
 * l'administrateur. Pas de suppression : un service se désactive. Les écritures passent par la file hors ligne
 * (`OutboxService`, SYS-07) : `queued` vaut vrai si la saisie attend le réseau.
 */
@Injectable({ providedIn: 'root' })
export class SalonCatalogService {
  private readonly http = inject(HttpClient);
  private readonly outbox = inject(OutboxService);
  private readonly baseUrl = '/api/v1/salon-services';

  list(query: SalonServiceListQuery): Observable<PageResponse<SalonServiceRDto>> {
    const { active, ...page } = query;
    let params = toHttpParams(page);
    if (active !== undefined) {
      params = params.set('active', String(active));
    }
    return this.http.get<PageResponse<SalonServiceRDto>>(this.baseUrl, { params });
  }

  getById(id: string): Observable<SalonServiceRDto> {
    return this.http.get<SalonServiceRDto>(`${this.baseUrl}/${id}`);
  }

  create(body: SalonServiceWDto): Observable<SubmitResult<SalonServiceRDto>> {
    return this.outbox.submit<SalonServiceRDto>('POST', this.baseUrl, body);
  }

  update(id: string, body: SalonServiceWDto): Observable<SubmitResult<SalonServiceRDto>> {
    return this.outbox.submit<SalonServiceRDto>('PUT', `${this.baseUrl}/${id}`, body);
  }

  setActive(id: string, active: boolean): Observable<SubmitResult<SalonServiceRDto>> {
    const body: SalonServiceStatusWDto = { active };
    return this.outbox.submit<SalonServiceRDto>('PATCH', `${this.baseUrl}/${id}/status`, body);
  }
}
