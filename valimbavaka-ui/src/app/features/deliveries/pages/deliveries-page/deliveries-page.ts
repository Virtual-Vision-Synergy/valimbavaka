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
import { Plus } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthStore } from '../../../../core/auth/auth-store';
import { ChipOption, FilterChips } from '../../../../shared/components/filter-chips/filter-chips';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { SidePanel, isPanelDocked } from '../../../../shared/components/side-panel/side-panel';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { formatAriary } from '../../../../shared/pipes/ariary-pipe';
import { DeliveryCard } from '../../components/delivery-card/delivery-card';
import { DeliveryDetail } from '../../components/delivery-detail/delivery-detail';
import { businessToday } from '../../data-access/delivery-mapping';
import {
  DELIVERY_STATUSES,
  DELIVERY_STATUS_FILTERS,
  DeliveryOrderStatus,
  DeliveryStatusFilter,
} from '../../data-access/delivery-models';
import { DeliveryService } from '../../data-access/delivery-service';

/**
 * Suivi des livraisons (LIV-01, LIV-06) : colonnes par statut pour un jour (aujourd'hui par défaut, un jour passé
 * donne son historique), filtres par tournée et par statut, détail avec l'historique de la commande. Tout vient de
 * l'API ; la vendeuse et l'administrateur créent une commande avec « Nouvelle commande » (LIV-01).
 */
@Component({
  selector: 'vvs-deliveries-page',
  imports: [
    RouterLink,
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

  private readonly docked = isPanelDocked();

  protected readonly icons = { Plus };
  protected readonly canCreate = inject(AuthStore).hasPermission('deliveries.create');
  /** Jour affiché (`yyyy-MM-dd`, Antananarivo). */
  protected readonly day = signal(businessToday());
  protected readonly board = rxResource({
    params: () => this.day(),
    stream: ({ params }) => this.deliveryService.board(params),
  });
  protected readonly round = signal('ALL');
  protected readonly status = signal<DeliveryStatusFilter>('ALL');

  protected readonly roundOptions = computed<readonly ChipOption<string>[]>(() => [
    { value: 'ALL', labelKey: 'deliveries.allRounds' },
    ...(this.board.value()?.rounds ?? []).map((round) => ({
      value: round.id,
      label: round.name === round.driverName ? round.name : `${round.name} · ${round.driverName}`,
    })),
  ]);
  /** Commandes de la tournée choisie, annulées comprises. */
  private readonly ofRound = computed(() => {
    const round = this.round();
    const all = this.board.value()?.deliveries ?? [];
    return round === 'ALL' ? all : all.filter((delivery) => delivery.roundId === round);
  });
  protected readonly statusOptions = computed<readonly ChipOption<DeliveryStatusFilter>[]>(() =>
    DELIVERY_STATUS_FILTERS.map((status) => ({
      value: status,
      labelKey: status === 'ALL' ? 'deliveries.allStatuses' : `deliveries.status.${status}`,
      count:
        status === 'ALL'
          ? this.ofRound().filter((delivery) => delivery.status !== 'CANCELLED').length
          : this.ofRound().filter((delivery) => delivery.status === status).length,
    })),
  );
  /** Colonnes affichées : les quatre du tableau (sans les annulées), ou la seule du statut choisi. */
  protected readonly columns = computed(() => {
    const status = this.status();
    const statuses: readonly DeliveryOrderStatus[] =
      status === 'ALL' ? DELIVERY_STATUSES : [status];
    return statuses.map((column) => ({
      status: column,
      items: this.ofRound().filter((delivery) => delivery.status === column),
    }));
  });
  protected readonly summary = computed(() => {
    const all = (this.board.value()?.deliveries ?? []).filter(
      (delivery) => delivery.status !== 'CANCELLED',
    );
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
    return (this.board.value()?.deliveries ?? []).filter(
      (delivery) => delivery.roundId === roundId && delivery.status !== 'CANCELLED',
    ).length;
  }

  /** Un champ date vidé garde le jour affiché. */
  protected pickDay(value: string): void {
    if (value) {
      this.day.set(value);
    }
  }
}
