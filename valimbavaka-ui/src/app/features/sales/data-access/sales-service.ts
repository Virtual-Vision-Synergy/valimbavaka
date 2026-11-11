import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageResponse } from '../../../shared/models/page-response';
import { SaleSummaryRDto, SalesQuery } from './sales-models';

/** Filtres envoyés seulement s'ils sont renseignés. */
const FILTERS = ['date', 'salesChannelId', 'sellerId'] as const;

/**
 * Ventes enregistrées (VTE-07) : `GET /api/v1/sales` (permission `sales.read`), une page de ventes d'un jour, les plus
 * récentes d'abord. L'administrateur lit toutes les ventes ; une vendeuse seulement celles qui lui sont créditées
 * (filtre imposé par le serveur). L'enregistrement passe par le poste de caisse (`PosService`, VTE-01).
 */
@Injectable({ providedIn: 'root' })
export class SalesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/sales';

  list(query: SalesQuery): Observable<PageResponse<SaleSummaryRDto>> {
    return this.http.get<PageResponse<SaleSummaryRDto>>(this.baseUrl, { params: params(query) });
  }
}

function params(query: SalesQuery): HttpParams {
  let result = toHttpParams(query);
  for (const key of FILTERS) {
    const value = query[key];
    if (value) {
      result = result.set(key, value);
    }
  }
  return result;
}
