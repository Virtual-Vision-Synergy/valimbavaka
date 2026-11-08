import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  linkedSignal,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { Lock } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthStore } from '../../../../core/auth/auth-store';
import { HasPermissionDirective } from '../../../../core/auth/has-permission-directive';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { StepItem, Stepper } from '../../../../shared/components/stepper/stepper';
import { TabItem, Tabs } from '../../../../shared/components/tabs/tabs';
import { ActivityFeedCard } from '../../components/activity-feed-card/activity-feed-card';
import { DashboardHero } from '../../components/dashboard-hero/dashboard-hero';
import { DashboardKpis } from '../../components/dashboard-kpis/dashboard-kpis';
import { RevenueChartCard } from '../../components/revenue-chart-card/revenue-chart-card';
import { RevenueSplitCard } from '../../components/revenue-split-card/revenue-split-card';
import { StockAlertsCard } from '../../components/stock-alerts-card/stock-alerts-card';
import { TopArticlesCard } from '../../components/top-articles-card/top-articles-card';
import { DashboardService } from '../../data-access/dashboard-service';
import { StockService } from '../../../stocks/data-access/stock-service';
import { Icon } from '../../../../shared/components/icon/icon';

export type DashboardTab = 'overview' | 'analysis';

const TABS: readonly TabItem<DashboardTab>[] = [
  { id: 'overview', labelKey: 'dashboard.tab.overview' },
  { id: 'analysis', labelKey: 'dashboard.tab.analysis' },
];

/**
 * Cockpit du jour (DSH-01 à DSH-04) : vue d'ensemble pour agir, analyse détaillée pour comprendre. Les alertes de
 * stock (DSH-03) viennent de `GET /api/v1/stocks/alerts`, la tendance du CA (DSH-02) de
 * `GET /api/v1/dashboard/revenue-trend` ; le reste reste sur l'agrégat de démonstration.
 */
@Component({
  selector: 'vvs-dashboard-page',
  imports: [
    RouterLink,
    TranslatePipe,
    HasPermissionDirective,
    PageHeader,
    ListState,
    Tabs,
    Stepper,
    DashboardHero,
    DashboardKpis,
    RevenueChartCard,
    RevenueSplitCard,
    StockAlertsCard,
    ActivityFeedCard,
    TopArticlesCard,
    Icon,
  ],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardPage {
  protected readonly icons = { Lock };
  private readonly router = inject(Router);
  private readonly authStore = inject(AuthStore);
  private readonly dashboardService = inject(DashboardService);
  private readonly stockService = inject(StockService);

  /** Paramètre `?tab=` (lien partageable vers un onglet). */
  readonly tab = input<string>();

  protected readonly tabs = TABS;
  protected readonly selectedTab = linkedSignal<DashboardTab>(() =>
    this.tab() === 'analysis' ? 'analysis' : 'overview',
  );
  protected readonly dashboard = rxResource({ stream: () => this.dashboardService.get() });
  protected readonly firstName = computed(() => this.authStore.currentUser()?.firstName ?? '');
  protected readonly canSeeSellers = computed(() => this.authStore.hasPermission('reports.read'));
  /** Graphique du CA (DSH-02) : réservé à l'administrateur (`reports.read`), sinon aucun appel. */
  protected readonly canSeeRevenueTrend = computed(() =>
    this.authStore.hasPermission('reports.read'),
  );
  /** Tendance du CA, lue à part de l'agrégat (DSH-01) : un échec n'empêche pas le reste du cockpit. */
  protected readonly revenueTrend = rxResource({
    params: () => (this.canSeeRevenueTrend() ? true : undefined),
    stream: () => this.dashboardService.revenueTrend(),
  });
  protected readonly revenueTrendValue = computed(() =>
    this.revenueTrend.hasValue() ? this.revenueTrend.value() : undefined,
  );
  protected readonly revenueTrendFailed = computed(() => this.revenueTrend.status() === 'error');
  /** Alertes de stock (DSH-03) : réservées à `stocks.read`, sinon aucun appel (le livreur n'y a pas droit). */
  protected readonly canSeeStockAlerts = computed(() =>
    this.authStore.hasPermission('stocks.read'),
  );
  /**
   * Articles sous leur seuil (STK-04), lus à part de l'agrégat du cockpit (DSH-01) : un échec n'empêche pas
   * le reste du cockpit de s'afficher. `params` indéfini = ressource au repos, aucune requête.
   */
  protected readonly stockAlerts = rxResource({
    params: () => (this.canSeeStockAlerts() ? true : undefined),
    stream: () => this.stockService.alerts(),
  });
  protected readonly stockAlertsPage = computed(() =>
    this.stockAlerts.hasValue() ? this.stockAlerts.value() : undefined,
  );
  protected readonly stockAlertsFailed = computed(() => this.stockAlerts.status() === 'error');
  protected readonly steps = computed<StepItem[]>(() =>
    (this.dashboard.value()?.steps ?? []).map((step) => ({
      labelKey: `dashboard.step.${step.code}.label`,
      captionKey: `dashboard.step.${step.code}.caption`,
      params: step.params,
      state: step.state,
    })),
  );

  protected selectTab(tab: DashboardTab): void {
    this.selectedTab.set(tab);
    void this.router.navigate([], {
      queryParams: { tab },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }
}
