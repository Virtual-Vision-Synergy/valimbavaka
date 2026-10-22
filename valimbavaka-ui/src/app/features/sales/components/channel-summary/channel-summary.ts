import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { SaleChannel } from '../../../../shared/models/commerce';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { SalesDayRDto } from '../../data-access/sales-models';

export type ChannelFilter = 'ALL' | SaleChannel;

/** Cartes de totaux par canal (VTE-07), qui servent aussi de filtre de la liste. */
@Component({
  selector: 'vvs-channel-summary',
  imports: [TranslatePipe, AriaryPipe],
  templateUrl: './channel-summary.html',
  styleUrl: './channel-summary.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChannelSummary {
  readonly day = input.required<SalesDayRDto>();
  readonly selected = model.required<ChannelFilter>();

  protected readonly cards = computed(() => {
    const day = this.day();
    return [
      { value: 'ALL' as ChannelFilter, amount: day.total, count: day.count, share: null },
      ...day.byChannel.map((item) => ({
        value: item.channel as ChannelFilter,
        amount: item.amount,
        count: item.count,
        share: day.total ? Math.round((item.amount / day.total) * 100) : 0,
      })),
    ];
  });
}
