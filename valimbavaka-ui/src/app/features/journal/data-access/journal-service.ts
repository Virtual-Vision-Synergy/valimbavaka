import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageQuery, PageResponse } from '../../../shared/models/page-response';
import { JournalEntryRDto } from './journal-models';

/** Lecture seule : le journal est append-only, l'API n'expose aucune écriture. */
@Injectable({ providedIn: 'root' })
export class JournalService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/journal';

  list(query: PageQuery): Observable<PageResponse<JournalEntryRDto>> {
    return this.http.get<PageResponse<JournalEntryRDto>>(this.baseUrl, {
      params: toHttpParams(query),
    });
  }
}
