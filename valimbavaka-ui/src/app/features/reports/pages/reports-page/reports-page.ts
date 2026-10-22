import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FileSpreadsheet, FileText } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../../../../shared/components/icon/icon';
import { KpiCard, KpiTrend } from '../../../../shared/components/kpi-card/kpi-card';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import {
  SegmentedControl,
  SegmentOption,
} from '../../../../shared/components/segmented-control/segmented-control';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { SALE_CHANNELS } from '../../../../shared/models/commerce';
import { ProductRange } from '../../../../shared/models/tone';
import { AriaryPipe, formatAriary } from '../../../../shared/pipes/ariary-pipe';
import { RangeDonut } from '../../components/range-donut/range-donut';
import { RevenueCompareChart } from '../../components/revenue-compare-chart/revenue-compare-chart';
import { REPORT_SELLERS } from '../../data-access/report-fixtures';
import { ReportFilterWDto, ReportKpiRDto, ReportPeriod } from '../../data-access/report-models';
import { ReportService } from '../../data-access/report-service';

const PERIODS: readonly SegmentOption<ReportPeriod>[] = [
  { value: 'DAY', labelKey: 'reports.period.DAY' },
  { value: 'MONTH', labelKey: 'reports.period.MONTH' },
  { value: 'YEAR', labelKey: 'reports.period.YEAR' },
];
const RANGES: readonly ProductRange[] = ['BOTANIKA', 'ZEZIKA', 'SOLIKA'];
const INITIAL: ReportFilterWDto = { period: 'MONTH', channel: 'ALL', range: 'ALL', seller: 'ALL' };

/** Rapports (RPT-01 à RPT-06) : filtres, indicateurs comparés, courbe du CA, gammes, meilleurs produits. */
@Component({
  selector: 'vvs-reports-page',
  imports: [
    TranslatePipe,
    AriaryPipe,
    Icon,
    KpiCard,
    ListState,
    PageHeader,
    SegmentedControl,
    RangeDonut,
    RevenueCompareChart,
  ],
  templateUrl: './reports-page.html',
  styleUrl: './reports-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportsPage {
  private readonly reportService = inject(ReportService);
  private readonly toast = inject(ToastService);

  protected readonly icons = { FileText, FileSpreadsheet };
  protected readonly periods = PERIODS;
  protected readonly channels = SALE_CHANNELS;
  protected readonly ranges = RANGES;
  protected readonly sellers = REPORT_SELLERS;
  protected readonly filter = signal<ReportFilterWDto>(INITIAL);
  protected readonly period = computed(() => this.filter().period);
  protected readonly report = rxResource({
    params: () => this.filter(),
    stream: ({ params }) => this.reportService.report(params),
  });
  protected readonly dirty = computed(() => {
    const { channel, range, seller } = this.filter();
    return channel !== 'ALL' || range !== 'ALL' || seller !== 'ALL';
  });

  protected setPeriod(period: ReportPeriod): void {
    this.filter.update((current) => ({ ...current, period }));
  }

  protected setFilter(key: 'channel' | 'range' | 'seller', value: string): void {
    this.filter.update((current) => ({ ...current, [key]: value }));
  }

  protected reset(): void {
    this.filter.update((current) => ({ ...INITIAL, period: current.period }));
  }

  protected exportAs(format: string): void {
    this.toast.show('reports.exported', { format }, 'info');
  }

  protected trend(kpi: ReportKpiRDto, unit: 'percent' | 'points'): KpiTrend {
    const diff = kpi.value - kpi.previous;
    const sign = diff >= 0 ? '+' : '−';
    const magnitude = Math.abs(diff);
    const text =
      unit === 'points'
        ? `${sign}${magnitude} pts`
        : `${sign}${((magnitude / (kpi.previous || 1)) * 100).toFixed(1).replace('.', ',')} %`;
    return { tone: diff >= 0 ? 'success' : 'danger', label: text };
  }

  protected compact(amount: number): string {
    return formatAriary(amount, 'compact');
  }
}
