import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageQuery, PageResponse } from '../../../shared/models/page-response';
import { CreateUserWDto, UpdateUserWDto, UserRDto } from './user-models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/users';

  list(query: PageQuery): Observable<PageResponse<UserRDto>> {
    return this.http.get<PageResponse<UserRDto>>(this.baseUrl, { params: toHttpParams(query) });
  }

  getById(id: string): Observable<UserRDto> {
    return this.http.get<UserRDto>(`${this.baseUrl}/${id}`);
  }

  create(body: CreateUserWDto): Observable<UserRDto> {
    return this.http.post<UserRDto>(this.baseUrl, body);
  }

  update(id: string, body: UpdateUserWDto): Observable<UserRDto> {
    return this.http.put<UserRDto>(`${this.baseUrl}/${id}`, body);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
