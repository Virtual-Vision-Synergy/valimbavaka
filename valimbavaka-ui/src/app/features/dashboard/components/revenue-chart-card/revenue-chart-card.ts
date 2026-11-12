import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language-service';
import { BarChart, ChartBar, ChartTick } from '../../../../shared/components/bar-chart/bar-chart';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { AriaryPipe, formatAriary } from '../../../../shared/pipes/ariary-pipe';
import { RevenueTrendRDto } from '../../data-access/dashboard-models';

/** Nombre de graduations visé sur l'axe vertical. */
const TARGET_TICKS = 4;
const NICE_STEPS = [1, 2, 5, 10];

/** Pas « rond » (1, 2 ou 5 × une puissance de dix) donnant environ 4 graduations ; 0,5 M quand tout est à 0. */
function tickStep(max: number): number {
  if (max <= 0) {
    return 500_000;
  }
  const raw = max / TARGET_TICKS;
  const magnitude = 10 ** Math.floor(Math.log10(raw));
  return (NICE_STEPS.find((factor) => factor * magnitude >= raw) ?? 10) * magnitude;
}

/** Montant court : « 1,28 M » dès un million, « 640 k » en dessous, « 0 » pour un jour sans vente. */
function shortAmount(amount: number): string {
  if (amount === 0) {
    return '0';
  }
  return formatAriary(amount, amount >= 1_000_000 ? 'compact' : 'kilo');
}

/**
 * Chiffre d'affaires des 7 derniers jours (DSH-02) : une barre par jour, aujourd'hui mis en avant, valeur exacte
 * dans l'infobulle au survol. Les données viennent de `GET /api/v1/dashboard/revenue-trend`, lues par la page.
 */
@Component({
  selector: 'vvs-revenue-chart-card',
  imports: [TranslatePipe, BarChart, ListState, AriaryPipe],
  templateUrl: './revenue-chart-card.html',
  styleUrl: './revenue-chart-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RevenueChartCard {
  private readonly language = inject(LanguageService);
  private readonly translate = inject(TranslateService);

  /** Absent tant que la lecture charge. */
  readonly trend = input<RevenueTrendRDto>();
  readonly failed = input(false);
  readonly retry = output();

  private readonly points = computed(() => this.trend()?.points ?? []);
  protected readonly total = computed(() =>
    this.points().reduce((sum, point) => sum + point.revenue, 0),
  );
  protected readonly average = computed(() =>
    Math.round(this.total() / Math.max(this.points().length, 1)),
  );
  protected readonly bars = computed<ChartBar[]>(() => {
    const points = this.points();
    return points.map((point, index) => ({
      label: this.dayLabel(point.date, index === points.length - 1),
      value: point.revenue,
      display: shortAmount(point.revenue),
      highlight: index === points.length - 1,
      tooltip: this.translate.instant('dashboard.chart.tooltip', {
        date: this.format(point.date, { weekday: 'long', day: 'numeric', month: 'long' }),
        amount: formatAriary(point.revenue),
        count: point.salesCount,
      }),
    }));
  });
  protected readonly ticks = computed<ChartTick[]>(() => {
    const highest = Math.max(...this.points().map((point) => point.revenue), 0);
    const step = tickStep(highest);
    const max = Math.ceil(highest / step) * step || step;
    return Array.from({ length: max / step + 1 }, (_, i) => max - i * step).map((value) => ({
      value,
      label: shortAmount(value),
    }));
  });

  private dayLabel(isoDate: string, isToday: boolean): string {
    return isToday
      ? this.translate.instant('dashboard.chart.today')
      : this.format(isoDate, { weekday: 'short' });
  }

  private format(isoDate: string, options: Intl.DateTimeFormatOptions): string {
    return new Intl.DateTimeFormat(this.language.current(), options).format(
      new Date(`${isoDate}T12:00:00`),
    );
  }
}
