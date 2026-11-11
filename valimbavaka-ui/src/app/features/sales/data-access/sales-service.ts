import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageResponse } from '../../../shared/models/page-response';
import { SaleDetailRDto, SaleSummaryRDto, SalesQuery } from './sales-models';

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

  /**
   * Détail d'une vente (VTE-09) : `GET /api/v1/sales/{id}/detail` (`sales.read`), lignes nommées, promotion, facture et
   * commission avec la règle appliquée. Une vendeuse n'obtient que ses ventes (404 sinon).
   */
  detail(saleId: string): Observable<SaleDetailRDto> {
    return this.http.get<SaleDetailRDto>(`${this.baseUrl}/${saleId}/detail`);
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
