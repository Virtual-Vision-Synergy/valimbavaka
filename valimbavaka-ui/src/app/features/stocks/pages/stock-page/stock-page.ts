import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FilePlus, Truck } from 'lucide';
import { of } from 'rxjs';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthStore } from '../../../../core/auth/auth-store';
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
import {
  LocationFilter,
  StockLevel,
  levelAt,
  quantityAt,
  thresholdAt,
} from '../../data-access/stock-levels';
import { StockService } from '../../data-access/stock-service';

type LevelFilter = 'ALL' | Exclude<StockLevel, 'OK'>;

/** Stocks par emplacement (STK-01 à STK-06) : projection du journal, alertes et réappro. */
@Component({
  selector: 'vvs-stock-page',
  imports: [
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
  ],
  templateUrl: './stock-page.html',
  styleUrl: './stock-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StockPage {
  private readonly stockService = inject(StockService);
  private readonly authStore = inject(AuthStore);
  private readonly toast = inject(ToastService);

  protected readonly icons = { FilePlus, Truck };
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
  protected readonly selectedRows = computed(() =>
    (this.stock.value() ?? []).filter((row) => this.selection().has(row.articleId)),
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

  /** Bon de réappro : par défaut, tous les articles sous le seuil de la vue courante. */
  protected startRestock(): void {
    const ids = this.rows()
      .filter((row) => row.level !== 'OK')
      .map((row) => row.articleId);
    this.selection.set(new Set(ids));
  }

  protected demoAction(): void {
    this.toast.show('common.demoAction', {}, 'info');
  }

  protected validateRestock(): void {
    this.toast.show('stocks.restock.validated', { count: this.selection().size });
    this.selection.set(new Set());
  }
}
