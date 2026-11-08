import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { ShortDatePipe } from '../../../../shared/pipes/short-date-pipe';
import {
  PROMOTION_STATUS_TONES,
  channelLabels,
  discountLabel,
  rangeLabels,
} from '../../data-access/promotion-labels';
import { PromotionReferences } from '../../data-access/promotion-models';
import { PromotionService } from '../../data-access/promotion-service';

/**
 * Fiche détaillée d'une promotion (PRO-04) : relue sur le serveur à l'ouverture, avec son statut du jour,
 * sa remise, son périmètre, ses canaux, sa période, son code et ses utilisations.
 */
@Component({
  selector: 'vvs-promotion-detail',
  imports: [TranslatePipe, ShortDatePipe, ListState, StatusBadge],
  templateUrl: './promotion-detail.html',
  styleUrl: './promotion-detail.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromotionDetail {
  private readonly promotionService = inject(PromotionService);

  readonly promotionId = input.required<string>();
  /** Gammes et canaux du référentiel, pour afficher des libellés plutôt que des identifiants. */
  readonly references = input.required<PromotionReferences>();

  protected readonly tones = PROMOTION_STATUS_TONES;
  protected readonly promotion = rxResource({
    params: () => this.promotionId(),
    stream: ({ params }) => this.promotionService.getById(params),
  });
  /** `value()` lève une exception tant que la ressource est en erreur : toujours passer par `hasValue()`. */
  private readonly loaded = computed(() =>
    this.promotion.hasValue() ? this.promotion.value() : undefined,
  );
  protected readonly badge = computed(() => {
    const promotion = this.loaded();
    return promotion ? discountLabel(promotion.kind, promotion.value) : '';
  });
  protected readonly channels = computed(() => {
    const promotion = this.loaded();
    return promotion ? channelLabels(promotion, this.references()) : [];
  });
  protected readonly ranges = computed(() => {
    const promotion = this.loaded();
    return promotion ? rangeLabels(promotion, this.references()) : [];
  });
}
