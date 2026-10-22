import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language-service';
import { BarChart, ChartBar, ChartTick } from '../../../../shared/components/bar-chart/bar-chart';
import {
  SegmentOption,
  SegmentedControl,
} from '../../../../shared/components/segmented-control/segmented-control';
import { AriaryPipe, formatAriary } from '../../../../shared/pipes/ariary-pipe';
import { DailyRevenueRDto, DashboardRDto, RevenueWindow } from '../../data-access/dashboard-models';

/** Pas de graduation de l'axe : 0,5 M par jour, 2 M par semaine. */
const TICK_STEP: Record<RevenueWindow, number> = { WEEK: 500_000, MONTH: 2_000_000 };

/** Chiffre d'affaires sur 7 jours ou 4 semaines (DSH-02) ; la période en cours est mise en avant. */
@Component({
  selector: 'vvs-revenue-chart-card',
  imports: [TranslatePipe, BarChart, SegmentedControl, AriaryPipe],
  templateUrl: './revenue-chart-card.html',
  styleUrl: './revenue-chart-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RevenueChartCard {
  private readonly language = inject(LanguageService);
  private readonly translate = inject(TranslateService);

  readonly data = input.required<DashboardRDto>();

  protected readonly window = signal<RevenueWindow>('WEEK');
  protected readonly windowOptions: readonly SegmentOption<RevenueWindow>[] = [
    { value: 'WEEK', labelKey: 'dashboard.chart.week' },
    { value: 'MONTH', labelKey: 'dashboard.chart.month' },
  ];
  private readonly series = computed<readonly DailyRevenueRDto[]>(() =>
    this.window() === 'WEEK' ? this.data().revenueLast7Days : this.data().revenueLast4Weeks,
  );
  protected readonly total = computed(() =>
    this.series().reduce((sum, day) => sum + day.amount, 0),
  );
  protected readonly average = computed(() =>
    Math.round(this.total() / Math.max(this.series().length, 1)),
  );
  protected readonly bars = computed<ChartBar[]>(() => {
    const series = this.series();
    return series.map((point, index) => ({
      label: this.pointLabel(point.date, index === series.length - 1),
      value: point.amount,
      display: formatAriary(point.amount, 'compact'),
      highlight: index === series.length - 1,
    }));
  });
  protected readonly ticks = computed<ChartTick[]>(() => {
    const step = TICK_STEP[this.window()];
    const max =
      Math.ceil(Math.max(...this.series().map((point) => point.amount), step) / step) * step;
    return Array.from({ length: max / step + 1 }, (_, i) => max - i * step).map((value) => ({
      value,
      label: value === 0 ? '0' : formatAriary(value, 'compact'),
    }));
  });

  private pointLabel(isoDate: string, isCurrent: boolean): string {
    if (isCurrent) {
      return this.translate.instant(
        this.window() === 'WEEK' ? 'dashboard.chart.today' : 'dashboard.chart.thisWeek',
      );
    }
    const date = new Date(`${isoDate}T12:00:00`);
    const options: Intl.DateTimeFormatOptions =
      this.window() === 'WEEK' ? { weekday: 'short' } : { day: 'numeric', month: 'short' };
    return new Intl.DateTimeFormat(this.language.current(), options).format(date);
  }
}
