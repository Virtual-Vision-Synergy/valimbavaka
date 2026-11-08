import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  linkedSignal,
  signal,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ChipOption, FilterChips } from '../../../../shared/components/filter-chips/filter-chips';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { Tone } from '../../../../shared/models/tone';
import { ShortDatePipe } from '../../../../shared/pipes/short-date-pipe';
import { InventoryStatus } from '../../data-access/inventory-models';
import { InventoryService } from '../../data-access/inventory-service';
import { closedOn } from '../../data-access/inventory-sheet';

export const HISTORY_PAGE_SIZE = 20;

type StatusFilter = InventoryStatus | 'ALL';

const STATUS_OPTIONS: readonly ChipOption<StatusFilter>[] = [
  { value: 'ALL', labelKey: 'inventory.history.allStatuses' },
  { value: 'CLOSED', labelKey: 'inventory.status.CLOSED' },
  { value: 'IN_PROGRESS', labelKey: 'inventory.status.IN_PROGRESS' },
  { value: 'CANCELLED', labelKey: 'inventory.status.CANCELLED' },
];

const STATUS_TONES: Readonly<Record<InventoryStatus, Tone>> = {
  IN_PROGRESS: 'info',
  CLOSED: 'success',
  CANCELLED: 'muted',
};

/**
 * Historique des sessions d'inventaire (INV-05) : toutes les sessions, les plus récentes d'abord, avec leur statut,
 * leur nombre d'articles et leur nombre d'écarts ; filtre par statut et pagination faits par le serveur.
 */
@Component({
  selector: 'vvs-inventory-history-page',
  imports: [
    RouterLink,
    TranslatePipe,
    ShortDatePipe,
    PageHeader,
    ListState,
    FilterChips,
    Pagination,
    StatusBadge,
  ],
  templateUrl: './inventory-history-page.html',
  styleUrl: './inventory-history-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InventoryHistoryPage {
  private readonly inventoryService = inject(InventoryService);

  protected readonly pageSize = HISTORY_PAGE_SIZE;
  protected readonly statusOptions = STATUS_OPTIONS;
  protected readonly tones = STATUS_TONES;
  protected readonly closedOn = closedOn;
  protected readonly status = signal<StatusFilter>('ALL');
  protected readonly page = linkedSignal({ source: this.status, computation: () => 1 });

  /** Un nouveau filtre ou une nouvelle page annule la requête précédente. */
  protected readonly sessions = rxResource({
    params: () => ({ page: this.page(), status: this.status() }),
    stream: ({ params }) =>
      this.inventoryService.list({
        page: params.page,
        size: HISTORY_PAGE_SIZE,
        sortBy: 'createdAt',
        sortDirection: 'DESC',
        status: params.status === 'ALL' ? undefined : params.status,
      }),
  });
  /** `value()` lève une exception tant que la ressource est en erreur : toujours passer par `hasValue()`. */
  protected readonly items = computed(() =>
    this.sessions.hasValue() ? this.sessions.value().items : [],
  );
}
