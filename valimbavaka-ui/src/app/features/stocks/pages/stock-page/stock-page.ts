import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { FilePlus, Truck, Undo2 } from 'lucide';
import { finalize, of } from 'rxjs';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthStore } from '../../../../core/auth/auth-store';
import { errorMessage } from '../../../../core/http/problem-detail';
import { ChipOption, FilterChips } from '../../../../shared/components/filter-chips/filter-chips';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { RangeTag } from '../../../../shared/components/range-tag/range-tag';
import { StockGauge } from '../../../../shared/components/stock-gauge/stock-gauge';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { STOCK_LOCATIONS } from '../../../../shared/data-access/demo-catalog';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { LocationCards } from '../../components/location-cards/location-cards';
import { RestockDraft } from '../../components/restock-draft/restock-draft';
import { ThresholdAlerts } from '../../components/threshold-alerts/threshold-alerts';
import {
  LocationFilter,
  StockLevel,
  levelAt,
  quantityAt,
  segmentsOf,
  thresholdAt,
} from '../../data-access/stock-levels';
import { StockService } from '../../data-access/stock-service';

type LevelFilter = 'ALL' | Exclude<StockLevel, 'OK'>;

/** Stocks par emplacement (STK-01 à STK-06) : projection du journal, alertes et réappro. */
@Component({
  selector: 'vvs-stock-page',
  imports: [
    RouterLink,
    TranslatePipe,
    AriaryPipe,
    Icon,
    PageHeader,
    ListState,
    FilterChips,
    RangeTag,
    StockGauge,
    LocationCards,
    RestockDraft,
    ThresholdAlerts,
  ],
  templateUrl: './stock-page.html',
  styleUrl: './stock-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StockPage {
  private readonly stockService = inject(StockService);
  private readonly authStore = inject(AuthStore);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly icons = { FilePlus, Truck, Undo2 };
  /** Retour de stock (TRF-03) : administrateur et vendeuse. */
  protected readonly canRecordReturn = this.authStore.hasPermission('transfers.return');
  protected readonly locations = STOCK_LOCATIONS;
  protected readonly stock = rxResource({ stream: () => this.stockService.list() });
  /** Valorisation de tout le stock par emplacement (STK-03) : administrateur seulement, sinon calcul local. */
  protected readonly valuation = rxResource({
    stream: () =>
      this.authStore.hasPermission('stocks.value') ? this.stockService.valuation() : of(null),
  });
  protected readonly totals = computed(() =>
    this.valuation.hasValue() ? this.valuation.value() : null,
  );
  /** Articles sous leur seuil (STK-04) : calculés par le serveur sur tout le stock, pas sur la page chargée. */
  protected readonly alerts = rxResource({ stream: () => this.stockService.alerts() });
  protected readonly location = signal<LocationFilter>('ALL');
  protected readonly level = signal<LevelFilter>('ALL');
  protected readonly selection = signal<ReadonlySet<string>>(new Set());

  protected readonly rows = computed(() => {
    const location = this.location();
    return (this.stock.value() ?? []).map((row) => ({
      ...row,
      quantity: quantityAt(row, location),
      threshold: thresholdAt(row, location),
      level: levelAt(row, location),
      value: quantityAt(row, 'ALL') * row.unitCost,
      /** Répartition par emplacement, seulement sur la vue « Tous » (STK-02). */
      segments: location === 'ALL' ? segmentsOf(row) : [],
    }));
  });
  protected readonly visibleRows = computed(() => {
    const level = this.level();
    return this.rows()
      .filter((row) => level === 'ALL' || row.level === level)
      .sort((a, b) => a.quantity / a.threshold - b.quantity / b.threshold);
  });
  protected readonly levelOptions = computed<readonly ChipOption<LevelFilter>[]>(() => {
    const rows = this.rows();
    return [
      { value: 'ALL', labelKey: 'stocks.level.ALL', count: rows.length },
      {
        value: 'LOW',
        labelKey: 'stocks.level.LOW',
        count: rows.filter((row) => row.level === 'LOW').length,
      },
      {
        value: 'OUT',
        labelKey: 'stocks.level.OUT',
        count: rows.filter((row) => row.level === 'OUT').length,
      },
    ];
  });
  /** Bon de réappro (STK-05) : réservé à `stocks.restock` (administrateur), le serveur décide. */
  protected readonly canRestock = computed(() => this.authStore.hasPermission('stocks.restock'));
  protected readonly generating = signal(false);
  /** Lignes du brouillon : alertes du serveur parmi les articles cochés ; un article au-dessus du seuil n'y est pas. */
  protected readonly selectedAlerts = computed(() =>
    (this.alerts.value()?.items ?? []).filter((alert) => this.selection().has(alert.articleId)),
  );

  protected toggle(id: string): void {
    this.selection.update((current) => {
      const next = new Set(current);
      if (!next.delete(id)) {
        next.add(id);
      }
      return next;
    });
  }

  /** Bon de réappro : par défaut, tous les articles sous leur seuil selon le serveur (STK-04). */
  protected startRestock(): void {
    const ids = (this.alerts.value()?.items ?? []).map((alert) => alert.articleId);
    this.selection.set(new Set(ids));
  }

  protected demoAction(): void {
    this.toast.show('common.demoAction', {}, 'info');
  }

  /** Le serveur génère le bon à partir des articles encore sous le seuil et confirme sa référence (STK-05). */
  protected validateRestock(): void {
    const ids = this.selectedAlerts().map((alert) => alert.articleId);
    this.generating.set(true);
    this.stockService
      .generateRestockOrder(ids)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.generating.set(false)),
      )
      .subscribe({
        next: (result) => {
          if (result.body) {
            const { reference, lines } = result.body;
            this.toast.show('stocks.restock.generated', { reference, count: lines.length });
          } else {
            this.toast.show('common.queuedSaved', {}, 'info');
          }
          this.selection.set(new Set());
        },
        error: (error: unknown) =>
          this.toast.show('stocks.restock.failed', { message: errorMessage(error) }, 'danger'),
      });
  }
}
