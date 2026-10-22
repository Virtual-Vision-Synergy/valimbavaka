import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { PROMOTIONS_FIXTURE } from './promotion-fixtures';
import { PromotionContextRDto, PromotionRDto, PromotionWDto } from './promotion-models';

/**
 * Promotions. Provisoire : données de démonstration ; à terme `GET /api/v1/promotions`
 * et `POST /api/v1/promotions` (le serveur contrôle les chevauchements et les codes).
 */
@Injectable({ providedIn: 'root' })
export class PromotionService {
  context(): Observable<PromotionContextRDto> {
    return demo(PROMOTIONS_FIXTURE);
  }

  /** Simulation : renvoie la promotion telle que le serveur la créerait. */
  create(promotion: PromotionWDto): Observable<PromotionRDto> {
    return demo(promotion).pipe(
      map((created) => ({
        id: `pro-new-${created.code ?? created.name}`,
        name: created.name,
        description: '',
        kind: created.kind,
        value: created.value,
        code: created.code,
        startsOn: created.startsOn,
        endsOn: created.endsOn,
        uses: 0,
        limit: created.limit,
        discountGranted: 0,
      })),
    );
  }
}
