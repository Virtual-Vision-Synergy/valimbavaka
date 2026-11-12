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
import { DashboardDemoRDto, DashboardRDto } from '../../data-access/dashboard-models';
import { DashboardService } from '../../data-access/dashboard-service';
import { StockService } from '../../../stocks/data-access/stock-service';
import { Icon } from '../../../../shared/components/icon/icon';

export type DashboardTab = 'overview' | 'analysis';

const TABS: readonly TabItem<DashboardTab>[] = [
  { id: 'overview', labelKey: 'dashboard.tab.overview' },
  { id: 'analysis', labelKey: 'dashboard.tab.analysis' },
];

/** Heure de Madagascar (« 13:05 ») d'un instant ISO : le serveur travaille dans ce fuseau. */
function clockTime(instant: string): string {
  return new Intl.DateTimeFormat('fr-FR', {
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'Indian/Antananarivo',
  }).format(new Date(instant));
}

/** Les zones simulées, avec la date, l'heure de lecture et les étapes alignées sur les vrais indicateurs (DSH-01). */
function alignedOnLive(demo: DashboardDemoRDto, live: DashboardRDto): DashboardDemoRDto {
  const { delivered, total } = live.deliveries;
  return {
    ...demo,
    businessDate: live.businessDate,
    updatedAt: clockTime(live.generatedAt),
    steps: demo.steps.map((step) => {
      if (step.code === 'SALES') {
        return { ...step, params: { tickets: live.sales.count } };
      }
      return step.code === 'DELIVERIES' ? { ...step, params: { done: delivered, total } } : step;
    }),
  };
}

/**
 * Cockpit du jour (DSH-01 à DSH-04) : vue d'ensemble pour agir, analyse détaillée pour comprendre. Les 4
 * indicateurs (DSH-01) viennent de `GET /api/v1/dashboard`, réservé à l'administrateur (`reports.read`) ; les alertes
 * de stock (DSH-03) de `GET /api/v1/stocks/alerts`. Les autres zones (`demoZones`) restent simulées : chacune sera
 * branchée par son ticket ; le graphique du CA (DSH-02) est lu sur `GET /api/v1/dashboard/revenue-trend` et la
 * répartition du CA (DSH-04) sur `GET /api/v1/dashboard/revenue-split`, chacun à part des indicateurs.
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
  /** Les indicateurs du jour sont ceux de l'administrateur : le serveur refuse les autres rôles (403). */
  protected readonly canSeeCockpit = computed(() => this.authStore.hasPermission('reports.read'));
  protected readonly dashboard = rxResource({
    params: () => (this.canSeeCockpit() ? true : undefined),
    stream: () => this.dashboardService.get(),
  });
  /** CA des 7 derniers jours (DSH-02), lu à part de l'agrégat : un échec n'empêche pas le reste de s'afficher. */
  protected readonly revenueTrend = rxResource({
    params: () => (this.canSeeCockpit() ? true : undefined),
    stream: () => this.dashboardService.revenueTrend(),
  });
  protected readonly revenueTrendValue = computed(() =>
    this.revenueTrend.hasValue() ? this.revenueTrend.value() : undefined,
  );
  protected readonly revenueTrendFailed = computed(() => this.revenueTrend.status() === 'error');
  /** Répartition du CA du jour (DSH-04), lue à part de l'agrégat : un échec n'empêche pas le reste de s'afficher. */
  protected readonly revenueSplit = rxResource({
    params: () => (this.canSeeCockpit() ? true : undefined),
    stream: () => this.dashboardService.revenueSplit(),
  });
  protected readonly revenueSplitValue = computed(() =>
    this.revenueSplit.hasValue() ? this.revenueSplit.value() : undefined,
  );
  protected readonly revenueSplitFailed = computed(() => this.revenueSplit.status() === 'error');
  protected readonly demoZones = rxResource({
    params: () => (this.canSeeCockpit() ? true : undefined),
    stream: () => this.dashboardService.demoZones(),
  });
  /** Indicateurs réels et zones simulées, prêts ensemble ; absent tant que l'un des deux charge. */
  protected readonly view = computed(() => {
    const live = this.dashboard.hasValue() ? this.dashboard.value() : undefined;
    const demo = this.demoZones.hasValue() ? this.demoZones.value() : undefined;
    return live && demo ? { live, demo: alignedOnLive(demo, live) } : undefined;
  });
  protected readonly firstName = computed(() => this.authStore.currentUser()?.firstName ?? '');
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
    (this.view()?.demo.steps ?? []).map((step) => ({
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
