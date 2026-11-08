import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ShortDatePipe } from '../../../../shared/pipes/short-date-pipe';
import { PROMOTION_STATUS_TONES, discountLabel } from '../../data-access/promotion-labels';
import { PromotionCardView, PromotionStatus } from '../../data-access/promotion-models';

/** Carte d'une promotion : remise, période, code, utilisations et remise accordée ; un clic ouvre sa fiche (PRO-04). */
@Component({
  selector: 'vvs-promotion-card',
  imports: [TranslatePipe, AriaryPipe, ShortDatePipe, StatusBadge],
  templateUrl: './promotion-card.html',
  styleUrl: './promotion-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromotionCard {
  readonly promotion = input.required<PromotionCardView>();
  readonly status = input.required<PromotionStatus>();
  /** Vrai si la fiche de cette promotion est ouverte. */
  readonly selected = input(false);
  /** Émis au clic sur la carte : la page ouvre la fiche détaillée. */
  readonly opened = output<void>();

  protected readonly tone = computed(() => PROMOTION_STATUS_TONES[this.status()]);
  /** Libellé court de la remise (« −15 % », « 0 Ar », « 3 = 2 »). */
  protected readonly badge = computed(() =>
    discountLabel(this.promotion().kind, this.promotion().value),
  );
  protected readonly usage = computed(() => {
    const { uses, limit } = this.promotion();
    return limit ? Math.min(100, (uses / limit) * 100) : uses > 0 ? 40 : 0;
  });
}
