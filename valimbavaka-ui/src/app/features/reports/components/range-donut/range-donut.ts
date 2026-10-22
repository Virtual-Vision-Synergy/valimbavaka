import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { RangeTag } from '../../../../shared/components/range-tag/range-tag';
import { ReportRangeRDto } from '../../data-access/report-models';

const RADIUS = 52;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

interface Arc {
  readonly item: ReportRangeRDto;
  readonly percent: number;
  readonly dash: string;
  readonly offset: number;
}

/** Répartition du chiffre d'affaires par gamme (RPT-04) : anneau SVG + légende chiffrée. */
@Component({
  selector: 'vvs-range-donut',
  imports: [TranslatePipe, AriaryPipe, RangeTag],
  templateUrl: './range-donut.html',
  styleUrl: './range-donut.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RangeDonut {
  readonly ranges = input.required<readonly ReportRangeRDto[]>();

  protected readonly total = computed(() =>
    this.ranges().reduce((sum, item) => sum + item.amount, 0),
  );
  protected readonly arcs = computed<readonly Arc[]>(() => {
    const total = this.total() || 1;
    let consumed = 0;
    return this.ranges().map((item) => {
      const share = item.amount / total;
      const arc: Arc = {
        item,
        percent: Math.round(share * 100),
        dash: `${share * CIRCUMFERENCE} ${CIRCUMFERENCE}`,
        offset: -consumed * CIRCUMFERENCE,
      };
      consumed += share;
      return arc;
    });
  });
}
