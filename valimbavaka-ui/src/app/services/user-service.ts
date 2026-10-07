import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '../models/api-response';
import { Page } from '../models/page';
import { CreateUserWDto, UpdateUserWDto, UserQuery, UserRDto } from '../models/user';
import { unwrapData } from '../utils/api-utils';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/users';

  getAll(query: UserQuery): Observable<Page<UserRDto>> {
    const params = new HttpParams()
      .set('search', query.search)
      .set('page', query.page)
      .set('size', query.size);
    return this.http
      .get<ApiResponse<Page<UserRDto>>>(this.baseUrl, { params })
      .pipe(map(unwrapData));
  }

  getById(id: string): Observable<UserRDto> {
    return this.http.get<ApiResponse<UserRDto>>(`${this.baseUrl}/${id}`).pipe(map(unwrapData));
  }

  create(request: CreateUserWDto): Observable<UserRDto> {
    return this.http.post<ApiResponse<UserRDto>>(this.baseUrl, request).pipe(map(unwrapData));
  }

  update(id: string, request: UpdateUserWDto): Observable<UserRDto> {
    return this.http
      .put<ApiResponse<UserRDto>>(`${this.baseUrl}/${id}`, request)
      .pipe(map(unwrapData));
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
