import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageQuery, PageResponse } from '../../../shared/models/page-response';
import {
  ArticleExportQuery,
  ArticleListQuery,
  ArticleRDto,
  ArticleStatusWDto,
  ArticleWDto,
  ProductionUsage,
} from './article-models';

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
    const params = withCriteria(toHttpParams(query), query);
    return this.http.get<PageResponse<ArticleRDto>>(this.baseUrl, { params });
  }

  /** Fichier xlsx de la liste (ART-08, administrateur) : mêmes critères que la liste, jamais le coût d'achat. */
  exportList(query: ArticleExportQuery): Observable<HttpResponse<Blob>> {
    let params = new HttpParams();
    if (query.search) {
      params = params.set('search', query.search);
    }
    return this.http.get(`${this.baseUrl}/exportable/export`, {
      params: withCriteria(params, query),
      observe: 'response',
      responseType: 'blob',
    });
  }

  /** Articles actifs utilisables en production (ART-04) : le serveur filtre par type, le client n'en décide pas. */
  listForProduction(
    usage: ProductionUsage,
    query: PageQuery,
  ): Observable<PageResponse<ArticleRDto>> {
    const params = toHttpParams(query).set('usage', usage);
    return this.http.get<PageResponse<ArticleRDto>>(`${this.baseUrl}/production`, { params });
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

function withCriteria(params: HttpParams, query: ArticleExportQuery): HttpParams {
  let result = params;
  for (const key of ['type', 'rangeId', 'supplierId'] as const) {
    const value = query[key];
    if (value) {
      result = result.set(key, value);
    }
  }
  return query.active === undefined ? result : result.set('active', query.active);
}
