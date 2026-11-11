import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ChannelDashboardQuery, ChannelDashboardRDto } from './channel-dashboard-models';

/**
 * Tableau de bord des ventes par canal (VTE-08) : `GET /api/v1/sales/channel-dashboard` (permission `reports.read`,
 * administrateur). Chiffre d'affaires, nombre de ventes validées et part du chiffre d'affaires de chaque canal.
 */
@Injectable({ providedIn: 'root' })
export class ChannelDashboardService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/v1/sales/channel-dashboard';

  get(query: ChannelDashboardQuery): Observable<ChannelDashboardRDto> {
    let params = new HttpParams();
    for (const key of ['from', 'to'] as const) {
      const value = query[key];
      if (value) {
        params = params.set(key, value);
      }
    }
    return this.http.get<ChannelDashboardRDto>(this.url, { params });
  }
}
