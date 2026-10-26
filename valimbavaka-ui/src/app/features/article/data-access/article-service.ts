import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageResponse } from '../../../shared/models/page-response';
import { ArticleListQuery, ArticleRDto, ArticleStatusWDto, ArticleWDto } from './article-models';

/** Pas de suppression : un article se désactive (JNL-02). */
@Injectable({ providedIn: 'root' })
export class ArticleService {
  private readonly http = inject(HttpClient);
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

  create(body: ArticleWDto): Observable<ArticleRDto> {
    return this.http.post<ArticleRDto>(this.baseUrl, body);
  }

  update(id: string, body: ArticleWDto): Observable<ArticleRDto> {
    return this.http.put<ArticleRDto>(`${this.baseUrl}/${id}`, body);
  }

  setActive(id: string, active: boolean): Observable<ArticleRDto> {
    const body: ArticleStatusWDto = { active };
    return this.http.patch<ArticleRDto>(`${this.baseUrl}/${id}/status`, body);
  }
}
