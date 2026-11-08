import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageResponse } from '../../../shared/models/page-response';
import {
  CommissionRuleListQuery,
  CommissionRuleRDto,
  CommissionRuleStatusWDto,
  CommissionRuleWDto,
} from './commission-rule-models';

/**
 * Règles de commission par canal (COM-01), réservées à l'administrateur. Pas de suppression : une règle se
 * désactive. Les écritures passent par la file hors ligne (`OutboxService`, SYS-07) : `queued` vaut vrai si la
 * saisie attend le réseau.
 */
@Injectable({ providedIn: 'root' })
export class CommissionRuleService {
  private readonly http = inject(HttpClient);
  private readonly outbox = inject(OutboxService);
  private readonly baseUrl = '/api/v1/commission-rules';

  list(query: CommissionRuleListQuery): Observable<PageResponse<CommissionRuleRDto>> {
    const { salesChannelId, active, ...page } = query;
    let params = toHttpParams(page);
    if (salesChannelId) {
      params = params.set('salesChannelId', salesChannelId);
    }
    if (active !== undefined) {
      params = params.set('active', String(active));
    }
    return this.http.get<PageResponse<CommissionRuleRDto>>(this.baseUrl, { params });
  }

  getById(id: string): Observable<CommissionRuleRDto> {
    return this.http.get<CommissionRuleRDto>(`${this.baseUrl}/${id}`);
  }

  create(body: CommissionRuleWDto): Observable<SubmitResult<CommissionRuleRDto>> {
    return this.outbox.submit<CommissionRuleRDto>('POST', this.baseUrl, body);
  }

  update(id: string, body: CommissionRuleWDto): Observable<SubmitResult<CommissionRuleRDto>> {
    return this.outbox.submit<CommissionRuleRDto>('PUT', `${this.baseUrl}/${id}`, body);
  }

  setActive(id: string, active: boolean): Observable<SubmitResult<CommissionRuleRDto>> {
    const body: CommissionRuleStatusWDto = { active };
    return this.outbox.submit<CommissionRuleRDto>('PATCH', `${this.baseUrl}/${id}/status`, body);
  }
}
