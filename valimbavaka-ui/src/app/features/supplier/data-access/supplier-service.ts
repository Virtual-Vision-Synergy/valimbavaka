import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageResponse } from '../../../shared/models/page-response';
import {
  SupplierListQuery,
  SupplierRDto,
  SupplierStatusWDto,
  SupplierWDto,
} from './supplier-models';

/** Pas de suppression : un fournisseur se désactive (JNL-02). */
@Injectable({ providedIn: 'root' })
export class SupplierService {
  private readonly http = inject(HttpClient);
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

  create(body: SupplierWDto): Observable<SupplierRDto> {
    return this.http.post<SupplierRDto>(this.baseUrl, body);
  }

  update(id: string, body: SupplierWDto): Observable<SupplierRDto> {
    return this.http.put<SupplierRDto>(`${this.baseUrl}/${id}`, body);
  }

  setActive(id: string, active: boolean): Observable<SupplierRDto> {
    const body: SupplierStatusWDto = { active };
    return this.http.patch<SupplierRDto>(`${this.baseUrl}/${id}/status`, body);
  }
}
