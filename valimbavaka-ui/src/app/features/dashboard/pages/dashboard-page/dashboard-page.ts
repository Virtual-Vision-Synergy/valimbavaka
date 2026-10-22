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
import { Icon } from '../../../../shared/components/icon/icon';

export type DashboardTab = 'overview' | 'analysis';

const TABS: readonly TabItem<DashboardTab>[] = [
  { id: 'overview', labelKey: 'dashboard.tab.overview' },
  { id: 'analysis', labelKey: 'dashboard.tab.analysis' },
];

/** Cockpit du jour (DSH-01 à DSH-04) : vue d'ensemble pour agir, analyse détaillée pour comprendre. */
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

  /** Paramètre `?tab=` (lien partageable vers un onglet). */
  readonly tab = input<string>();

  protected readonly tabs = TABS;
  protected readonly selectedTab = linkedSignal<DashboardTab>(() =>
    this.tab() === 'analysis' ? 'analysis' : 'overview',
  );
  protected readonly dashboard = rxResource({ stream: () => this.dashboardService.get() });
  protected readonly firstName = computed(() => this.authStore.currentUser()?.firstName ?? '');
  protected readonly canSeeSellers = computed(() => this.authStore.hasPermission('reports.read'));
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
