import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ReportRangeRDto, ReportRangesRDto } from '../../data-access/report-models';

const RADIUS = 52;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

interface Arc {
  readonly item: ReportRangeRDto;
  /** Part en %, à une décimale, avec la virgule française : « 66,7 ». */
  readonly percent: string;
  readonly dash: string;
  readonly offset: number;
}

/**
 * Répartition du chiffre d'affaires par gamme (RPT-04) : anneau SVG + légende chiffrée. Les parts viennent de l'API
 * (une décimale, total exactement 100,0) : le composant ne les recalcule pas.
 */
@Component({
  selector: 'vvs-range-donut',
  imports: [TranslatePipe, AriaryPipe],
  templateUrl: './range-donut.html',
  styleUrl: './range-donut.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RangeDonut {
  readonly ranges = input.required<ReportRangesRDto>();

  protected readonly arcs = computed<readonly Arc[]>(() => {
    let consumed = 0;
    return this.ranges().items.map((item) => {
      const fraction = item.share / 100;
      const arc: Arc = {
        item,
        percent: item.share.toFixed(1).replace('.', ','),
        dash: `${fraction * CIRCUMFERENCE} ${CIRCUMFERENCE}`,
        offset: -consumed * CIRCUMFERENCE,
      };
      consumed += fraction;
      return arc;
    });
  });
}
