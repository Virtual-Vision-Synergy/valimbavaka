import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '../models/api-response';
import { Page } from '../models/page';
import { RoleRDto } from '../models/role';
import { unwrapData } from '../utils/api-utils';

@Injectable({ providedIn: 'root' })
export class RoleService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/roles';

  getAll(): Observable<RoleRDto[]> {
    return this.http.get<ApiResponse<Page<RoleRDto>>>(this.baseUrl).pipe(
      map(unwrapData),
      map((page) => page.content),
    );
  }
}
