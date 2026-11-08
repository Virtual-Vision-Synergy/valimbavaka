import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map, of } from 'rxjs';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { toHttpParams } from '../../../shared/models/http-params';
import { PageResponse } from '../../../shared/models/page-response';
import { ReferentialService } from '../../referential/data-access/referential-service';
import { PROMOTION_SCOPES_FIXTURE } from './promotion-fixtures';
import {
  PromotionContext,
  PromotionDraft,
  PromotionListQuery,
  PromotionRDto,
  PromotionReferences,
} from './promotion-models';
import { toPromotionWDto } from './promotion-request';

/**
 * Promotions. Les lectures (liste paginée filtrée par statut, fiche) passent par `HttpClient` (PRO-04) ;
 * la création passe par `POST /api/v1/promotions` via la file hors ligne (SYS-07).
 */
@Injectable({ providedIn: 'root' })
export class PromotionService {
  private readonly http = inject(HttpClient);
  private readonly outbox = inject(OutboxService);
  private readonly referentials = inject(ReferentialService);
  private readonly baseUrl = '/api/v1/promotions';

  /** Le statut n'est envoyé que s'il est choisi : sans statut, toutes les promotions. */
  list(query: PromotionListQuery): Observable<PageResponse<PromotionRDto>> {
    const params = toHttpParams(query);
    return this.http.get<PageResponse<PromotionRDto>>(this.baseUrl, {
      params: query.status ? params.set('status', query.status) : params,
    });
  }

  getById(id: string): Observable<PromotionRDto> {
    return this.http.get<PromotionRDto>(`${this.baseUrl}/${id}`);
  }

  /** Référentiels du formulaire et de la fiche (gammes, canaux) et choix du périmètre. */
  context(): Observable<PromotionContext> {
    return forkJoin({
      fixture: of(PROMOTION_SCOPES_FIXTURE),
      ranges: this.referentials.listProductRanges(),
      channels: this.referentials.listSalesChannels(),
    }).pipe(map(({ fixture, ranges, channels }) => ({ ...fixture, ranges, channels })));
  }

  /** `queued` vaut vrai si la saisie attend le réseau ; sinon `body` porte la promotion créée. */
  create(
    draft: PromotionDraft,
    refs: PromotionReferences,
  ): Observable<SubmitResult<PromotionRDto>> {
    return this.outbox.submit<PromotionRDto>('POST', this.baseUrl, toPromotionWDto(draft, refs));
  }
}
