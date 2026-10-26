import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { toHttpParams } from '../../../shared/models/http-params';
import { OutboxService } from '../../../core/offline/outbox-service';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { PageResponse } from '../../../shared/models/page-response';
import {
  SupplierListQuery,
  SupplierRDto,
  SupplierStatusWDto,
  SupplierWDto,
} from './supplier-models';

/**
 * Pas de suppression : un fournisseur se désactive (JNL-02). Les lectures passent par `HttpClient`, les
 * écritures par la file hors ligne (`OutboxService`, SYS-07) : `queued` vaut vrai si la saisie attend le réseau.
 */
@Injectable({ providedIn: 'root' })
export class SupplierService {
  private readonly http = inject(HttpClient);
  private readonly outbox = inject(OutboxService);
  private readonly baseUrl = '/api/v1/suppliers';

  /** Le type n'est envoyé que s'il est choisi : sans type, tous les fournisseurs. */
  list(query: SupplierListQuery): Observable<PageResponse<SupplierRDto>> {
    const params = toHttpParams(query);
    return this.http.get<PageResponse<SupplierRDto>>(this.baseUrl, {
      params: query.type ? params.set('type', query.type) : params,
    });
  }

  getById(id: string): Observable<SupplierRDto> {
    return this.http.get<SupplierRDto>(`${this.baseUrl}/${id}`);
  }

  create(body: SupplierWDto): Observable<SubmitResult<SupplierRDto>> {
    return this.outbox.submit<SupplierRDto>('POST', this.baseUrl, body);
  }

  update(id: string, body: SupplierWDto): Observable<SubmitResult<SupplierRDto>> {
    return this.outbox.submit<SupplierRDto>('PUT', `${this.baseUrl}/${id}`, body);
  }

  setActive(id: string, active: boolean): Observable<SubmitResult<SupplierRDto>> {
    const body: SupplierStatusWDto = { active };
    return this.outbox.submit<SupplierRDto>('PATCH', `${this.baseUrl}/${id}/status`, body);
  }
}
