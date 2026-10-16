import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageResponse } from '../../../shared/models/page-response';
import { RoleRDto } from './user-models';

/** Les rôles sont un référentiel court (3 valeurs) : une seule page suffit. */
const ROLES_PAGE = { page: 1, size: 100, sortBy: 'name' } as const;

@Injectable({ providedIn: 'root' })
export class RoleService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/roles';

  listAll(): Observable<RoleRDto[]> {
    return this.http
      .get<PageResponse<RoleRDto>>(this.baseUrl, { params: toHttpParams(ROLES_PAGE) })
      .pipe(map((page) => page.items));
  }
}
