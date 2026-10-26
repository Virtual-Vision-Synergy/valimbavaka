import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  linkedSignal,
  signal,
} from '@angular/core';
import { rxResource, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Plus } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { HasPermissionDirective } from '../../../../core/auth/has-permission-directive';
import { ChipOption, FilterChips } from '../../../../shared/components/filter-chips/filter-chips';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { SearchField } from '../../../../shared/components/search-field/search-field';
import { SidePanel, isPanelDocked } from '../../../../shared/components/side-panel/side-panel';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { SupplierRDto, SupplierType } from '../../../supplier/data-access/supplier-models';
import { SupplierService } from '../../../supplier/data-access/supplier-service';
import { SupplierDetail } from '../../components/supplier-detail/supplier-detail';
import { SUPPLIER_TYPE_TONES } from '../../data-access/supplier-tones';

export const SUPPLIER_PAGE_SIZE = 8;
const SEARCH_DEBOUNCE_MS = 300;
const TYPE_OPTIONS: readonly ChipOption<SupplierType | 'ALL'>[] = [
  { value: 'ALL', labelKey: 'supplier.filter.allTypes' },
  { value: 'LOCAL', labelKey: 'supplier.type.LOCAL' },
  { value: 'FOREIGN', labelKey: 'supplier.type.FOREIGN' },
];

/**
 * Liste des fournisseurs (FRS-01, FRS-03) : recherche et filtre par type faits par le serveur,
 * pagination serveur, fiche à droite (informations, articles fournis, activation).
 */
@Component({
  selector: 'vvs-supplier-list-page',
  imports: [
    RouterLink,
    HasPermissionDirective,
    TranslatePipe,
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
  private readonly docked = isPanelDocked();

  protected readonly icons = { Plus };
  protected readonly pageSize = SUPPLIER_PAGE_SIZE;
  protected readonly typeTones = SUPPLIER_TYPE_TONES;
  protected readonly typeOptions = TYPE_OPTIONS;
  protected readonly query = signal('');
  protected readonly type = signal<SupplierType | 'ALL'>('ALL');
  /** Recherche envoyée au serveur : rognée et temporisée pour ne pas interroger à chaque frappe. */
  private readonly search = toSignal(
    toObservable(this.query).pipe(
      debounceTime(SEARCH_DEBOUNCE_MS),
      distinctUntilChanged((a, b) => a.trim() === b.trim()),
    ),
    { initialValue: '' },
  );
  protected readonly page = linkedSignal({
    source: () => ({ type: this.type(), search: this.search().trim() }),
    computation: () => 1,
  });

  /** Une nouvelle page ou un nouveau filtre annule la requête précédente. */
  protected readonly suppliers = rxResource({
    params: () => ({ page: this.page(), type: this.type(), search: this.search().trim() }),
    stream: ({ params }) =>
      this.supplierService.list({
        page: params.page,
        size: SUPPLIER_PAGE_SIZE,
        sortBy: 'name',
        sortDirection: 'ASC',
        search: params.search,
        type: params.type === 'ALL' ? undefined : params.type,
      }),
  });
  protected readonly items = computed(() => this.suppliers.value()?.items ?? []);
  protected readonly isFiltered = computed(() => this.type() !== 'ALL' || !!this.search().trim());
  /** Fournisseur affiché à droite : conservé après un rechargement tant qu'il est dans la liste. */
  protected readonly selectedId = linkedSignal<readonly SupplierRDto[], string | null>({
    source: this.items,
    computation: (items, previous) => {
      const kept = previous?.value ?? null;
      if (kept && items.some((supplier) => supplier.id === kept)) {
        return kept;
      }
      return this.docked ? (items[0]?.id ?? null) : null;
    },
  });
  protected readonly selectedName = computed(
    () => this.items().find((supplier) => supplier.id === this.selectedId())?.name ?? '',
  );
}
