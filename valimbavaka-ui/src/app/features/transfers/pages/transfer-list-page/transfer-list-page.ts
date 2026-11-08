import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  linkedSignal,
  signal,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { MoveRight, Plus, Undo2 } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { ChipOption, FilterChips } from '../../../../shared/components/filter-chips/filter-chips';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { SidePanel } from '../../../../shared/components/side-panel/side-panel';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { TransferDetail } from '../../components/transfer-detail/transfer-detail';
import {
  TRANSFER_ORIGINS,
  TransferListQuery,
  TransferOrigin,
} from '../../data-access/transfer-models';
import { TransferService } from '../../data-access/transfer-service';
import { TRANSFER_STATUS_TONES } from '../../data-access/transfer-tones';
import { WhenPipe } from '../../data-access/when-pipe';

export const TRANSFER_PAGE_SIZE = 9;

/**
 * Historique des transferts de stock (TRF-01) : référence, trajet, origine, date, statut et émetteur, paginé et
 * filtré par origine côté serveur. Le clic sur un numéro ouvre le détail du bon (TRF-04) dans le panneau latéral.
 */
@Component({
  selector: 'vvs-transfer-list-page',
  imports: [
    RouterLink,
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
  private readonly router = inject(Router);

  protected readonly icons = { MoveRight, Plus, Undo2 };
  protected readonly pageSize = TRANSFER_PAGE_SIZE;
  protected readonly tones = TRANSFER_STATUS_TONES;
  /** Origine reçue dans l'adresse (`/transfers?origin=MORNING`, lien de l'ouverture de journée, TRF-02). */
  readonly origin = input<string>();
  protected readonly originFilter = linkedSignal<TransferOrigin | 'ALL'>(
    () => TRANSFER_ORIGINS.find((value) => value === this.origin()) ?? 'ALL',
  );
  protected readonly page = linkedSignal({ source: this.originFilter, computation: () => 1 });
  protected readonly transfers = rxResource({
    params: (): TransferListQuery => {
      const origin = this.originFilter();
      const query = { page: this.page(), size: TRANSFER_PAGE_SIZE };
      return origin === 'ALL' ? query : { ...query, origin };
    },
    stream: ({ params }) => this.transferService.list(params),
  });
  protected readonly originOptions: readonly ChipOption<TransferOrigin | 'ALL'>[] = [
    { value: 'ALL', labelKey: 'transfers.origin.ALL' },
    ...TRANSFER_ORIGINS.map((value) => ({ value, labelKey: `transfers.filter.${value}` })),
  ];
  protected readonly rows = computed(() => this.transfers.value()?.items ?? []);
  protected readonly total = computed(() => this.transfers.value()?.totalCount ?? 0);
  protected readonly selectedId = signal<string | null>(null);
  protected readonly selectedNumber = computed(
    () => this.rows().find((item) => item.id === this.selectedId())?.number ?? '',
  );

  /** « Créer un retour » depuis le détail : ouvre le formulaire de retour de stock (TRF-03). */
  protected openReturn(): void {
    void this.router.navigateByUrl('/transfers/return');
  }

  protected demoAction(): void {
    this.toast.show('common.demoAction', {}, 'info');
  }
}
