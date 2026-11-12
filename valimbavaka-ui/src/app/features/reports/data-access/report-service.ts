import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ReportQuery, ReportRDto } from './report-models';

/**
 * Rapports (RPT-01 à RPT-03) : `GET /api/v1/reports` (permission `reports.read`, administrateur). Chiffre d'affaires,
 * ventes validées et panier moyen du jour, du mois ou de l'année, filtrés par canal et par gamme, comparés à la
 * période précédente (valeur et variation en %), avec la courbe du CA (un point par jour, ou par mois pour une année).
 */
@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/v1/reports';

  get(query: ReportQuery): Observable<ReportRDto> {
    let params = new HttpParams();
    for (const key of ['granularity', 'date', 'salesChannelId', 'rangeId'] as const) {
      const value = query[key];
      if (value) {
        params = params.set(key, value);
      }
    }
    return this.http.get<ReportRDto>(this.url, { params });
  }
}
