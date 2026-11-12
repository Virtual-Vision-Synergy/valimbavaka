import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FileSpreadsheet, FileText } from 'lucide';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Icon } from '../../../../shared/components/icon/icon';
import { KpiCard, KpiTrend } from '../../../../shared/components/kpi-card/kpi-card';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import {
  SegmentedControl,
  SegmentOption,
} from '../../../../shared/components/segmented-control/segmented-control';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { RangeDonut } from '../../components/range-donut/range-donut';
import { RevenueTrendChart } from '../../components/revenue-trend-chart/revenue-trend-chart';
import { ReferentialService } from '../../../referential/data-access/referential-service';
import { localIsoDate } from '../../../sales/data-access/sales-mapping';
import { ReportGranularity, ReportQuery, ReportRDto } from '../../data-access/report-models';
import { ReportService } from '../../data-access/report-service';

const QUANTITY = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 3 });

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
 * Rapports (RPT-01 à RPT-06). La synthèse (CA, ventes, panier moyen), sa comparaison à la période précédente, la
 * courbe d'évolution du CA, la répartition par gamme, le classement des articles les plus vendus et les filtres
 * période, canal et gamme sont branchés sur l'API (RPT-01 à RPT-05) ; seuls les boutons d'export restent à
 * brancher (RPT-06).
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
    RevenueTrendChart,
  ],
  templateUrl: './reports-page.html',
  styleUrl: './reports-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportsPage {
  private readonly reportService = inject(ReportService);
  private readonly referentialService = inject(ReferentialService);
  private readonly toast = inject(ToastService);
  private readonly translate = inject(TranslateService);

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

  /** Badge de variation en % ; `null` = pas de période précédente à comparer (pas de pourcentage). */
  protected trend(variation: number | null): KpiTrend {
    if (variation === null) {
      return { tone: 'muted', label: this.translate.instant('reports.variation.none') };
    }
    const magnitude = Math.abs(variation).toFixed(1).replace('.', ',');
    if (variation === 0) {
      return { tone: 'muted', label: `${magnitude} %` };
    }
    const sign = variation > 0 ? '+' : '−';
    return { tone: variation > 0 ? 'success' : 'danger', label: `${sign}${magnitude} %` };
  }

  /**
   * Écart entre le CA de la synthèse et celui des gammes (remises appliquées à toute la vente, non réparties par
   * gamme) ; 0 quand il n'y en a pas, ou quand un filtre gamme restreint la synthèse.
   */
  protected rangesGap(report: ReportRDto): number {
    const gap = report.sales.revenue - report.ranges.revenue;
    return report.rangeId === null && Math.abs(gap) >= 0.005 ? Math.abs(gap) : 0;
  }

  /** Quantité vendue : séparateur français, jusqu'à 3 décimales (les articles se vendent aussi au poids). */
  protected quantity(value: number): string {
    return QUANTITY.format(value);
  }

  /** Part du CA en %, une décimale fixe (« 9,1 % »). */
  protected percent(value: number): string {
    return `${value.toFixed(1).replace('.', ',')} %`;
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
