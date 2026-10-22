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
import { SidePanel, isPanelDocked } from '../../../../shared/components/side-panel/side-panel';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { formatAriary } from '../../../../shared/pipes/ariary-pipe';
import { DeliveryCard } from '../../components/delivery-card/delivery-card';
import { DeliveryDetail } from '../../components/delivery-detail/delivery-detail';
import { DELIVERY_STATUSES } from '../../data-access/delivery-models';
import { DeliveryService } from '../../data-access/delivery-service';

/** Suivi des livraisons du jour (LIV-01, LIV-06, LIV-07) : colonnes par statut et détail. */
@Component({
  selector: 'vvs-deliveries-page',
  imports: [
    TranslatePipe,
    Icon,
    PageHeader,
    ListState,
    FilterChips,
    DeliveryCard,
    DeliveryDetail,
    SidePanel,
  ],
  templateUrl: './deliveries-page.html',
  styleUrl: './deliveries-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeliveriesPage {
  private readonly deliveryService = inject(DeliveryService);
  private readonly toast = inject(ToastService);

  private readonly docked = isPanelDocked();

  protected readonly icons = { Plus };
  protected readonly board = rxResource({ stream: () => this.deliveryService.board() });
  protected readonly round = signal('ALL');

  protected readonly roundOptions = computed<readonly ChipOption<string>[]>(() => [
    { value: 'ALL', labelKey: 'deliveries.allRounds' },
    ...(this.board.value()?.rounds ?? []).map((round) => ({
      value: round.id,
      label: `${round.name} · ${round.driverName}`,
    })),
  ]);
  protected readonly visible = computed(() => {
    const round = this.round();
    const all = this.board.value()?.deliveries ?? [];
    return round === 'ALL' ? all : all.filter((delivery) => delivery.roundId === round);
  });
  protected readonly columns = computed(() =>
    DELIVERY_STATUSES.map((status) => ({
      status,
      items: this.visible().filter((delivery) => delivery.status === status),
    })),
  );
  protected readonly summary = computed(() => {
    const all = this.board.value()?.deliveries ?? [];
    const collected = all
      .filter((delivery) => delivery.status === 'DELIVERED')
      .reduce((sum, delivery) => sum + delivery.amount, 0);
    return {
      count: all.length,
      rounds: this.board.value()?.rounds.length ?? 0,
      collected: formatAriary(collected),
      total: formatAriary(all.reduce((sum, delivery) => sum + delivery.amount, 0)),
    };
  });
  protected readonly selectedId = linkedSignal(() =>
    this.docked
      ? (this.board.value()?.deliveries.find((delivery) => delivery.status === 'FAILED')?.id ??
        null)
      : null,
  );
  protected readonly selected = computed(() =>
    this.board.value()?.deliveries.find((delivery) => delivery.id === this.selectedId()),
  );

  protected stopCount(roundId: string | null): number {
    return (this.board.value()?.deliveries ?? []).filter((delivery) => delivery.roundId === roundId)
      .length;
  }

  protected demoAction(): void {
    this.toast.show('common.demoAction', {}, 'info');
  }
}
