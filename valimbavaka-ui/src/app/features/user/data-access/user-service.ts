import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageQuery, PageResponse } from '../../../shared/models/page-response';
import { CreateUserWDto, UpdateUserWDto, UserRDto, UserStatusWDto } from './user-models';

/**
 * Comptes utilisateurs. La création reste un appel direct : elle porte un mot de passe, qui ne doit jamais
 * être conservé dans la file hors ligne (IndexedDB). Modification et activation passent par `OutboxService`.
 */
@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly outbox = inject(OutboxService);
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

  update(id: string, body: UpdateUserWDto): Observable<SubmitResult<UserRDto>> {
    return this.outbox.submit<UserRDto>('PUT', `${this.baseUrl}/${id}`, body);
  }

  setActive(id: string, active: boolean): Observable<SubmitResult<UserRDto>> {
    const body: UserStatusWDto = { active };
    return this.outbox.submit<UserRDto>('PATCH', `${this.baseUrl}/${id}/status`, body);
  }
}
