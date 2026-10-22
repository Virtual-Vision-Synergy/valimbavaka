import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  linkedSignal,
  signal,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Plus } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { ChipOption, FilterChips } from '../../../../shared/components/filter-chips/filter-chips';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { SearchField } from '../../../../shared/components/search-field/search-field';
import { SidePanel, isPanelDocked } from '../../../../shared/components/side-panel/side-panel';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { SupplierDetail } from '../../components/supplier-detail/supplier-detail';
import { ShortDatePipe } from '../../../../shared/pipes/short-date-pipe';
import { SUPPLIER_TYPES, SupplierType } from '../../data-access/supplier-models';
import { SupplierService } from '../../data-access/supplier-service';
import { LONG_LEAD_TIME_DAYS, SUPPLIER_TYPE_TONES } from '../../data-access/supplier-tones';

export const SUPPLIER_PAGE_SIZE = 8;

/** Liste des fournisseurs (FRS-01) : recherche, filtre par type, fiche à droite. */
@Component({
  selector: 'vvs-supplier-list-page',
  imports: [
    TranslatePipe,
    ShortDatePipe,
    Icon,
    PageHeader,
    ListState,
    FilterChips,
    SearchField,
    Pagination,
    StatusBadge,
    SidePanel,
    SupplierDetail,
  ],
  templateUrl: './supplier-list-page.html',
  styleUrl: './supplier-list-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SupplierListPage {
  private readonly supplierService = inject(SupplierService);
  private readonly toast = inject(ToastService);
  private readonly docked = isPanelDocked();

  protected readonly icons = { Plus };
  protected readonly pageSize = SUPPLIER_PAGE_SIZE;
  protected readonly longLead = LONG_LEAD_TIME_DAYS;
  protected readonly typeTones = SUPPLIER_TYPE_TONES;
  protected readonly suppliers = rxResource({ stream: () => this.supplierService.list() });
  protected readonly query = signal('');
  protected readonly type = signal<SupplierType | 'ALL'>('ALL');

  protected readonly typeOptions = computed<readonly ChipOption<SupplierType | 'ALL'>[]>(() => {
    const all = this.suppliers.value() ?? [];
    return [
      { value: 'ALL', labelKey: 'suppliers.type.ALL', count: all.length },
      ...SUPPLIER_TYPES.map((value) => ({
        value,
        labelKey: `suppliers.type.${value}`,
        count: all.filter((supplier) => supplier.type === value).length,
      })),
    ];
  });
  protected readonly filtered = computed(() => {
    const needle = this.query().trim().toLowerCase();
    const type = this.type();
    return (this.suppliers.value() ?? []).filter(
      (supplier) =>
        (type === 'ALL' || supplier.type === type) &&
        [supplier.name, supplier.city, supplier.taxId, supplier.code].some((field) =>
          field.toLowerCase().includes(needle),
        ),
    );
  });
  protected readonly page = linkedSignal({ source: this.filtered, computation: () => 1 });
  protected readonly pageRows = computed(() =>
    this.filtered().slice((this.page() - 1) * SUPPLIER_PAGE_SIZE, this.page() * SUPPLIER_PAGE_SIZE),
  );
  protected readonly averageLead = computed(() => {
    const all = this.suppliers.value() ?? [];
    return all.length
      ? Math.round(all.reduce((sum, item) => sum + item.leadTimeDays, 0) / all.length)
      : 0;
  });
  protected readonly selectedId = linkedSignal(() =>
    this.docked ? (this.suppliers.value()?.[0]?.id ?? null) : null,
  );
  protected readonly selectedName = computed(
    () => this.suppliers.value()?.find((supplier) => supplier.id === this.selectedId())?.name ?? '',
  );

  protected demoAction(): void {
    this.toast.show('common.demoAction', {}, 'info');
  }
}
