import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { demo } from '../../../shared/data-access/demo';
import { ReferentialService } from '../../referential/data-access/referential-service';
import { PROMOTIONS_FIXTURE } from './promotion-fixtures';
import {
  PromotionContext,
  PromotionDraft,
  PromotionRDto,
  PromotionReferences,
} from './promotion-models';
import { toPromotionWDto } from './promotion-request';

/**
 * Promotions. La création passe par `POST /api/v1/promotions` via la file hors ligne (SYS-07) ; la liste
 * reste en données de démonstration jusqu'à PRO-04 (liste et détail).
 */
@Injectable({ providedIn: 'root' })
export class PromotionService {
  private readonly outbox = inject(OutboxService);
  private readonly referentials = inject(ReferentialService);
  private readonly baseUrl = '/api/v1/promotions';

  context(): Observable<PromotionContext> {
    return forkJoin({
      fixture: demo(PROMOTIONS_FIXTURE),
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
