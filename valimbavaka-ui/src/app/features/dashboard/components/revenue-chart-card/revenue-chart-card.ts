import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language-service';
import { BarChart, ChartBar, ChartTick } from '../../../../shared/components/bar-chart/bar-chart';
import { ListState } from '../../../../shared/components/list-state/list-state';
import {
  SegmentOption,
  SegmentedControl,
} from '../../../../shared/components/segmented-control/segmented-control';
import { AriaryFormat, AriaryPipe, formatAriary } from '../../../../shared/pipes/ariary-pipe';
import {
  RevenuePointRDto,
  RevenueTrendRDto,
  RevenueWindow,
} from '../../data-access/dashboard-models';

/** Pas minimal de l'axe (1 000 Ar) : une semaine sans vente garde une échelle lisible. */
const MIN_STEP = 1_000;
/** Nombre visé d'intervalles sur l'axe vertical. */
const TARGET_TICKS = 4;

/** Pas « rond » (1, 2, 2,5 ou 5 × 10ⁿ) donnant environ quatre graduations jusqu'au maximum. */
export function niceStep(max: number): number {
  const raw = Math.max(max / TARGET_TICKS, MIN_STEP);
  const magnitude = 10 ** Math.floor(Math.log10(raw));
  const factor = [1, 2, 2.5, 5, 10].find((candidate) => candidate * magnitude >= raw) ?? 10;
  return factor * magnitude;
}

/**
 * Chiffre d'affaires sur 7 jours ou 4 semaines (DSH-02), calculé par le serveur : une barre par jour (ou par
 * semaine), la période en cours mise en avant, une infobulle au survol avec la date et le montant exact.
 * `trend` absent = chargement, `failed` = erreur avec réessai.
 */
@Component({
  selector: 'vvs-revenue-chart-card',
  imports: [TranslatePipe, BarChart, ListState, SegmentedControl, AriaryPipe],
  templateUrl: './revenue-chart-card.html',
  styleUrl: './revenue-chart-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RevenueChartCard {
  private readonly language = inject(LanguageService);
  private readonly translate = inject(TranslateService);

  readonly trend = input<RevenueTrendRDto>();
  readonly failed = input(false);
  readonly retry = output();

  protected readonly window = signal<RevenueWindow>('WEEK');
  protected readonly windowOptions: readonly SegmentOption<RevenueWindow>[] = [
    { value: 'WEEK', labelKey: 'dashboard.chart.week' },
    { value: 'MONTH', labelKey: 'dashboard.chart.month' },
  ];
  private readonly series = computed<readonly RevenuePointRDto[]>(() => {
    const trend = this.trend();
    if (!trend) {
      return [];
    }
    return this.window() === 'WEEK' ? trend.last7Days : trend.last4Weeks;
  });
  protected readonly total = computed(() =>
    this.series().reduce((sum, point) => sum + point.amount, 0),
  );
  protected readonly average = computed(() =>
    Math.round(this.total() / Math.max(this.series().length, 1)),
  );
  private readonly step = computed(() =>
    niceStep(Math.max(0, ...this.series().map((point) => point.amount))),
  );
  /** Montants en millions dès que l'échelle dépasse 1 M, sinon en milliers (« 26 k »). */
  private readonly format = computed<AriaryFormat>(() =>
    this.step() * TARGET_TICKS >= 1_000_000 ? 'compact' : 'kilo',
  );
  protected readonly bars = computed<ChartBar[]>(() => {
    const series = this.series();
    return series.map((point, index) => ({
      label: this.pointLabel(point.date, index === series.length - 1),
      value: point.amount,
      display: formatAriary(point.amount, this.format()),
      highlight: index === series.length - 1,
      tooltip: this.tooltip(point),
    }));
  });
  protected readonly ticks = computed<ChartTick[]>(() => {
    const step = this.step();
    const highest = Math.max(...this.series().map((point) => point.amount), step);
    const max = Math.ceil(highest / step) * step;
    return Array.from({ length: Math.round(max / step) + 1 }, (_, i) => max - i * step).map(
      (value) => ({
        value,
        label: value === 0 ? '0' : formatAriary(value, this.format()),
      }),
    );
  });

  private pointLabel(isoDate: string, isCurrent: boolean): string {
    if (isCurrent) {
      return this.translate.instant(
        this.window() === 'WEEK' ? 'dashboard.chart.today' : 'dashboard.chart.thisWeek',
      );
    }
    const options: Intl.DateTimeFormatOptions =
      this.window() === 'WEEK' ? { weekday: 'short' } : { day: 'numeric', month: 'short' };
    return this.formatDate(isoDate, options);
  }

  /** « jeudi 5 novembre : 125 000 Ar » ou « Semaine du 2 novembre : 3 000 Ar ». */
  private tooltip(point: RevenuePointRDto): string {
    const week = this.window() === 'MONTH';
    const date = this.formatDate(
      point.date,
      week ? { day: 'numeric', month: 'long' } : { weekday: 'long', day: 'numeric', month: 'long' },
    );
    return this.translate.instant(
      week ? 'dashboard.chart.tooltipWeek' : 'dashboard.chart.tooltipDay',
      { date, amount: formatAriary(point.amount) },
    );
  }

  private formatDate(isoDate: string, options: Intl.DateTimeFormatOptions): string {
    return new Intl.DateTimeFormat(this.language.current(), options).format(
      new Date(`${isoDate}T12:00:00`),
    );
  }
}
