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
import { AriaryPipe, formatAriary } from '../../../../shared/pipes/ariary-pipe';
import { RangeDonut } from '../../components/range-donut/range-donut';
import { RevenueCompareChart } from '../../components/revenue-compare-chart/revenue-compare-chart';
import { ReferentialService } from '../../../referential/data-access/referential-service';
import { localIsoDate } from '../../../sales/data-access/sales-mapping';
import { ReportDemoService } from '../../data-access/report-demo-service';
import { ReportGranularity, ReportKpiRDto, ReportQuery } from '../../data-access/report-models';
import { ReportService } from '../../data-access/report-service';

const PERIODS: readonly SegmentOption<ReportGranularity>[] = [
  { value: 'DAY', labelKey: 'reports.period.DAY' },
  { value: 'MONTH', labelKey: 'reports.period.MONTH' },
  { value: 'YEAR', labelKey: 'reports.period.YEAR' },
];

/** Filtres du rapport : `''` = pas de restriction (tous les canaux, toutes les gammes). */
interface ReportFilter {
  readonly granularity: ReportGranularity;
  readonly date: string;
  readonly salesChannelId: string;
  readonly rangeId: string;
}

const initialFilter = (): ReportFilter => ({
  granularity: 'MONTH',
  date: localIsoDate(new Date()),
  salesChannelId: '',
  rangeId: '',
});

/**
 * Rapports (RPT-01 à RPT-06). La synthèse (CA, ventes, panier moyen) et les filtres période, canal et gamme sont
 * branchés sur l'API (RPT-01) ; les indicateurs comparés, la courbe, les gammes et le classement restent des
 * données de démonstration jusqu'à RPT-02 à RPT-05.
 */
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
  private readonly demoService = inject(ReportDemoService);
  private readonly referentialService = inject(ReferentialService);
  private readonly toast = inject(ToastService);

  protected readonly icons = { FileText, FileSpreadsheet };
  protected readonly periods = PERIODS;
  protected readonly today = localIsoDate(new Date());
  protected readonly filter = signal<ReportFilter>(initialFilter());
  protected readonly granularity = computed(() => this.filter().granularity);
  protected readonly channels = rxResource({
    stream: () => this.referentialService.listSalesChannels(),
  });
  protected readonly ranges = rxResource({
    stream: () => this.referentialService.listProductRanges(),
  });
  protected readonly summary = rxResource({
    params: () => toQuery(this.filter()),
    stream: ({ params }) => this.reportService.get(params),
  });
  protected readonly report = rxResource({
    params: () => this.filter().granularity,
    stream: ({ params }) => this.demoService.report({ granularity: params }),
  });
  protected readonly dirty = computed(() => {
    const { salesChannelId, rangeId } = this.filter();
    return salesChannelId !== '' || rangeId !== '';
  });

  protected setGranularity(granularity: ReportGranularity): void {
    this.filter.update((current) => ({ ...current, granularity }));
  }

  protected setDate(date: string): void {
    this.filter.update((current) => ({ ...current, date: date || this.today }));
  }

  protected setFilter(key: 'salesChannelId' | 'rangeId', value: string): void {
    this.filter.update((current) => ({ ...current, [key]: value }));
  }

  protected reset(): void {
    this.filter.update((current) => ({ ...current, salesChannelId: '', rangeId: '' }));
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

function toQuery(filter: ReportFilter): ReportQuery {
  return {
    granularity: filter.granularity,
    date: filter.date,
    salesChannelId: filter.salesChannelId,
    rangeId: filter.rangeId,
  };
}
