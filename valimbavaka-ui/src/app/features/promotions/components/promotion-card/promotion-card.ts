import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { AriaryPipe, formatAriary } from '../../../../shared/pipes/ariary-pipe';
import { ShortDatePipe } from '../../../../shared/pipes/short-date-pipe';
import { PromotionRDto, PromotionStatus } from '../../data-access/promotion-models';
import { Tone } from '../../../../shared/models/tone';

const STATUS_TONES: Readonly<Record<PromotionStatus, Tone>> = {
  ACTIVE: 'success',
  SCHEDULED: 'info',
  ENDED: 'muted',
};

/** Carte d'une promotion : remise, période, code, utilisations et remise accordée. */
@Component({
  selector: 'vvs-promotion-card',
  imports: [TranslatePipe, AriaryPipe, ShortDatePipe, StatusBadge],
  templateUrl: './promotion-card.html',
  styleUrl: './promotion-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromotionCard {
  readonly promotion = input.required<PromotionRDto>();
  readonly status = input.required<PromotionStatus>();

  protected readonly tone = computed(() => STATUS_TONES[this.status()]);
  /** Libellé court de la remise (« −15 % », « 0 Ar », « 3 = 2 »). */
  protected readonly badge = computed(() => {
    const { kind, value } = this.promotion();
    switch (kind) {
      case 'PERCENT':
        return `−${value} %`;
      case 'AMOUNT':
        return value === 0 ? '0 Ar' : `−${formatAriary(value)}`;
      case 'BUY_X':
        return `${value} = ${value - 1}`;
    }
  });
  protected readonly usage = computed(() => {
    const { uses, limit } = this.promotion();
    return limit ? Math.min(100, (uses / limit) * 100) : uses > 0 ? 40 : 0;
  });
}
