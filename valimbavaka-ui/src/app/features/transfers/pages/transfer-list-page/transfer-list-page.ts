import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  linkedSignal,
  signal,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MoveRight, Plus, Undo2 } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { ChipOption, FilterChips } from '../../../../shared/components/filter-chips/filter-chips';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { SidePanel, isPanelDocked } from '../../../../shared/components/side-panel/side-panel';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { TransferDetail } from '../../components/transfer-detail/transfer-detail';
import { TRANSFER_ORIGINS, TransferOrigin } from '../../data-access/transfer-models';
import { TransferService } from '../../data-access/transfer-service';
import { TRANSFER_STATUS_TONES } from '../../data-access/transfer-tones';
import { WhenPipe } from '../../data-access/when-pipe';

export const TRANSFER_PAGE_SIZE = 9;

/** Transferts de stock (TRF-01 à TRF-04) : mouvements entre entrepôt, boutique, livraison et salon. */
@Component({
  selector: 'vvs-transfer-list-page',
  imports: [
    TranslatePipe,
    WhenPipe,
    Icon,
    PageHeader,
    ListState,
    FilterChips,
    Pagination,
    StatusBadge,
    SidePanel,
    TransferDetail,
  ],
  templateUrl: './transfer-list-page.html',
  styleUrl: './transfer-list-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TransferListPage {
  private readonly transferService = inject(TransferService);
  private readonly toast = inject(ToastService);
  private readonly docked = isPanelDocked();

  protected readonly icons = { MoveRight, Plus, Undo2 };
  protected readonly pageSize = TRANSFER_PAGE_SIZE;
  protected readonly tones = TRANSFER_STATUS_TONES;
  protected readonly transfers = rxResource({ stream: () => this.transferService.list() });
  protected readonly origin = signal<TransferOrigin | 'ALL'>('ALL');

  protected readonly originOptions = computed<readonly ChipOption<TransferOrigin | 'ALL'>[]>(() => {
    const all = this.transfers.value() ?? [];
    return [
      { value: 'ALL', labelKey: 'transfers.origin.ALL' },
      ...TRANSFER_ORIGINS.map((value) => ({
        value,
        labelKey: `transfers.filter.${value}`,
        count: all.filter((transfer) => transfer.origin === value).length,
      })),
    ];
  });
  protected readonly filtered = computed(() => {
    const origin = this.origin();
    return (this.transfers.value() ?? []).filter(
      (transfer) => origin === 'ALL' || transfer.origin === origin,
    );
  });
  protected readonly page = linkedSignal({ source: this.filtered, computation: () => 1 });
  protected readonly pageRows = computed(() =>
    this.filtered().slice((this.page() - 1) * TRANSFER_PAGE_SIZE, this.page() * TRANSFER_PAGE_SIZE),
  );
  protected readonly selectedId = linkedSignal(() =>
    this.docked
      ? (this.transfers.value()?.find((item) => item.number === 'TRF-0092')?.id ?? null)
      : null,
  );
  protected readonly selectedNumber = computed(
    () => this.transfers.value()?.find((item) => item.id === this.selectedId())?.number ?? '',
  );

  protected demoAction(): void {
    this.toast.show('common.demoAction', {}, 'info');
  }
}
