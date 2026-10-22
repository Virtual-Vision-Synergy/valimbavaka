import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { RangeTag } from '../../../../shared/components/range-tag/range-tag';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { DashboardRDto } from '../../data-access/dashboard-models';

/** Répartition du chiffre d'affaires du jour par gamme puis par canal (DSH-04). */
@Component({
  selector: 'vvs-revenue-split-card',
  imports: [TranslatePipe, RangeTag, AriaryPipe],
  templateUrl: './revenue-split-card.html',
  styleUrl: './revenue-split-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RevenueSplitCard {
  readonly data = input.required<DashboardRDto>();

  protected readonly ranges = computed(() => withShare(this.data().revenueByRange));
  protected readonly channels = computed(() => withShare(this.data().revenueByChannel));
}

function withShare<T extends { amount: number }>(items: readonly T[]): (T & { share: number })[] {
  const total = items.reduce((sum, item) => sum + item.amount, 0) || 1;
  return items.map((item) => ({ ...item, share: Math.round((item.amount / total) * 100) }));
}
