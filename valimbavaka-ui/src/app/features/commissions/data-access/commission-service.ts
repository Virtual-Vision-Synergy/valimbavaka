import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageQuery, PageResponse } from '../../../shared/models/page-response';
import {
  CommissionFollowUpRDto,
  CommissionListQuery,
  CommissionRDto,
  CommissionRecalculationRDto,
  CommissionRecalculationWDto,
  CommissionValidationRDto,
  CommissionValidationWDto,
  SellerCommissionRDto,
} from './commission-models';

/**
 * Commissions. `list` : commissions calculées par le serveur (COM-02), toutes vendeuses, réservé à l'administrateur
 * (`commissions.read-all`). `sellers` et `validate` : statut à valider / validée par vendeuse et validation par
 * l'administrateur (`commissions.validate`, COM-03). `followUp` et `followUpLines` : suivi du mois et objectif
 * mensuel (COM-04) ; une vendeuse ne reçoit que les siens (le serveur impose son identifiant), l'administrateur
 * désigne la vendeuse. `recalculate` : recalcul d'une période par l'administrateur (`commissions.recalculate`, COM-05).
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

  /** Recalcule les commissions calculées de la période ; les validées et annulées ne sont jamais touchées (COM-05). */
  recalculate(request: CommissionRecalculationWDto): Observable<CommissionRecalculationRDto> {
    return this.http.post<CommissionRecalculationRDto>(`${this.baseUrl}/recalculation`, request);
  }

  /** Totaux du mois `AAAA-MM` et avancement vers l'objectif mensuel (COM-04). */
  followUp(month: string, sellerId?: string): Observable<CommissionFollowUpRDto> {
    return this.http.get<CommissionFollowUpRDto>(`${this.baseUrl}/follow-up`, {
      params: followUpParams(month, sellerId),
    });
  }

  /** Détail par vente du mois (base, taux, commission, statut), annulées comprises (COM-04). */
  followUpLines(
    query: PageQuery,
    month: string,
    sellerId?: string,
  ): Observable<PageResponse<CommissionRDto>> {
    const params = followUpParams(month, sellerId, toHttpParams(query));
    return this.http.get<PageResponse<CommissionRDto>>(`${this.baseUrl}/follow-up/lines`, {
      params,
    });
  }
}

function followUpParams(month: string, sellerId?: string, base = new HttpParams()): HttpParams {
  const params = base.set('month', month);
  return sellerId ? params.set('sellerId', sellerId) : params;
}
