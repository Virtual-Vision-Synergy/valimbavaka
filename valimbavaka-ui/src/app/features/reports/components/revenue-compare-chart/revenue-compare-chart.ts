import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { formatAriary } from '../../../../shared/pipes/ariary-pipe';
import { ReportPointRDto } from '../../data-access/report-models';

/** Hauteur de repère « jolie » au-dessus du maximum (pas de 500 k, 1 M, 5 M…). */
function niceMax(value: number): number {
  const step = 10 ** Math.floor(Math.log10(Math.max(value, 1)));
  return Math.ceil(value / step) * step;
}

/**
 * Barres appariées période courante / précédente (RPT-03). Décoratif pour les lecteurs d'écran,
 * qui lisent le tableau de données équivalent (WCAG 1.1.1) ; défilement horizontal sur mobile.
 */
@Component({
  selector: 'vvs-revenue-compare-chart',
  imports: [TranslatePipe],
  templateUrl: './revenue-compare-chart.html',
  styleUrl: './revenue-compare-chart.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RevenueCompareChart {
  readonly caption = input.required<string>();
  readonly points = input.required<readonly ReportPointRDto[]>();
  readonly currentLabel = input.required<string>();
  readonly previousLabel = input.required<string>();

  protected readonly max = computed(() =>
    niceMax(Math.max(...this.points().flatMap((p) => [p.current, p.previous]), 1)),
  );
  protected readonly ticks = computed(() =>
    [0, 0.25, 0.5, 0.75, 1].map((ratio) => ({
      ratio: ratio * 100,
      label: ratio === 0 ? '0' : formatAriary(this.max() * ratio, 'compact'),
    })),
  );

  protected percent(value: number): number {
    return (value / this.max()) * 100;
  }

  protected format(value: number): string {
    return formatAriary(Math.round(value));
  }
}
