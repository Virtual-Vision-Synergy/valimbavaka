import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageResponse } from '../../../shared/models/page-response';
import { SalesChannelRDto } from './user-models';

/** Les canaux de vente sont un référentiel court : une seule page, triée par libellé. */
const SALES_CHANNELS_PAGE = { page: 1, size: 100, sortBy: 'label' } as const;

/** Lecture des canaux proposés au formulaire utilisateur (une feature n'importe pas le code d'une autre, §2.4). */
@Injectable({ providedIn: 'root' })
export class SalesChannelService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/sales-channels';

  listAll(): Observable<SalesChannelRDto[]> {
    return this.http
      .get<PageResponse<SalesChannelRDto>>(this.baseUrl, { params: toHttpParams(SALES_CHANNELS_PAGE) })
      .pipe(map((page) => page.items));
  }
}
