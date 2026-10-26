import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageResponse } from '../../../shared/models/page-response';
import { ArticleListQuery, ArticleRDto, ArticleStatusWDto, ArticleWDto } from './article-models';

/**
 * Pas de suppression : un article se désactive (JNL-02). Lectures par `HttpClient`, écritures par la file
 * hors ligne (`OutboxService`, SYS-07) : `queued` vaut vrai si la saisie attend le réseau.
 */
@Injectable({ providedIn: 'root' })
export class ArticleService {
  private readonly http = inject(HttpClient);
  private readonly outbox = inject(OutboxService);
  private readonly baseUrl = '/api/v1/articles';

  /** Les critères ne sont envoyés que s'ils sont choisis : sans critère, tous les articles. */
  list(query: ArticleListQuery): Observable<PageResponse<ArticleRDto>> {
    let params = toHttpParams(query);
    for (const key of ['type', 'rangeId', 'supplierId'] as const) {
      const value = query[key];
      if (value) {
        params = params.set(key, value);
      }
    }
    if (query.active !== undefined) {
      params = params.set('active', query.active);
    }
    return this.http.get<PageResponse<ArticleRDto>>(this.baseUrl, { params });
  }

  getById(id: string): Observable<ArticleRDto> {
    return this.http.get<ArticleRDto>(`${this.baseUrl}/${id}`);
  }

  create(body: ArticleWDto): Observable<SubmitResult<ArticleRDto>> {
    return this.outbox.submit<ArticleRDto>('POST', this.baseUrl, body);
  }

  update(id: string, body: ArticleWDto): Observable<SubmitResult<ArticleRDto>> {
    return this.outbox.submit<ArticleRDto>('PUT', `${this.baseUrl}/${id}`, body);
  }

  setActive(id: string, active: boolean): Observable<SubmitResult<ArticleRDto>> {
    const body: ArticleStatusWDto = { active };
    return this.outbox.submit<ArticleRDto>('PATCH', `${this.baseUrl}/${id}/status`, body);
  }
}
