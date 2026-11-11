import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageQuery, PageResponse } from '../../../shared/models/page-response';
import { statementFixture } from './commission-fixtures';
import {
  CommissionListQuery,
  CommissionRDto,
  CommissionStatementRDto,
  CommissionValidationRDto,
  CommissionValidationWDto,
  SellerCommissionRDto,
} from './commission-models';

/**
 * Commissions. `list` : commissions calculées par le serveur (COM-02), toutes vendeuses, réservé à l'administrateur
 * (`commissions.read-all`). `sellers` et `validate` : statut à valider / validée par vendeuse et validation par
 * l'administrateur (`commissions.validate`, COM-03). `statement` : relevé de la vendeuse connectée, encore en
 * démonstration (COM-04).
 */
@Injectable({ providedIn: 'root' })
export class CommissionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/commissions';

  list(query: CommissionListQuery): Observable<PageResponse<CommissionRDto>> {
    const { sellerId, from, to, ...page } = query;
    let params = toHttpParams(page);
    if (sellerId) {
      params = params.set('sellerId', sellerId);
    }
    if (from) {
      params = params.set('from', from);
    }
    if (to) {
      params = params.set('to', to);
    }
    return this.http.get<PageResponse<CommissionRDto>>(this.baseUrl, { params });
  }

  /** Vendeuses avec leurs commissions à valider et validées sur les journées `[from, to]` (COM-03). */
  sellers(
    query: PageQuery,
    from: string,
    to: string,
  ): Observable<PageResponse<SellerCommissionRDto>> {
    const params = toHttpParams(query).set('from', from).set('to', to);
    return this.http.get<PageResponse<SellerCommissionRDto>>(`${this.baseUrl}/sellers`, { params });
  }

  /** Valide les commissions calculées d'une vendeuse sur la période ; une commission validée est définitive. */
  validate(request: CommissionValidationWDto): Observable<CommissionValidationRDto> {
    return this.http.post<CommissionValidationRDto>(`${this.baseUrl}/validation`, request);
  }

  statement(month: string): Observable<CommissionStatementRDto> {
    return demo(statementFixture(month));
  }
}
