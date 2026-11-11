import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { errorMessage } from '../../../../core/http/problem-detail';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { isUndoable } from '../../data-access/delivery-mapping';
import { DeliveryRDto } from '../../data-access/delivery-models';
import { DeliveryService } from '../../data-access/delivery-service';
import { DELIVERY_STATUS_TONES } from '../../data-access/delivery-tones';

/** Le délai d'annulation (5 min) est relu toutes les 30 s pour retirer le bouton « Annuler » à temps. */
const CLOCK_REFRESH_MS = 30_000;

/**
 * Historique des arrêts terminés de la journée, du plus récent au plus ancien (LIV-02). Une livraison ou un échec de
 * moins de 5 minutes peut être annulé : la commande retrouve son statut d'avant (LIV-05).
 */
@Component({
  selector: 'vvs-driver-history-page',
  imports: [TranslatePipe, AriaryPipe, ListState, StatusBadge],
  templateUrl: './driver-history-page.html',
  styleUrl: './driver-history-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DriverHistoryPage {
  private readonly deliveryService = inject(DeliveryService);
  private readonly toast = inject(ToastService);
  protected readonly round = rxResource({ stream: () => this.deliveryService.myRound() });
  private readonly now = signal(Date.now());
  protected readonly tones = DELIVERY_STATUS_TONES;
  protected readonly done = computed(() =>
    (this.round.value()?.stops ?? [])
      .filter((stop) => stop.status === 'DELIVERED' || stop.status === 'FAILED')
      .sort((a, b) => b.time.localeCompare(a.time)),
  );

  /** Arrêts dont la dernière action peut encore être annulée (le serveur reste juge du délai). */
  protected readonly undoable = computed(
    () =>
      new Set(
        this.done()
          .filter((stop) => isUndoable(stop, this.now()))
          .map((stop) => stop.id),
      ),
  );
  protected readonly undoing = signal<string | null>(null);

  constructor() {
    const timer = setInterval(() => this.now.set(Date.now()), CLOCK_REFRESH_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  protected undo(stop: DeliveryRDto): void {
    this.undoing.set(stop.id);
    this.deliveryService.undoLastAction(stop.id).subscribe({
      next: () => this.onUndone(stop),
      error: (error: unknown) => this.onRefused(stop, error),
    });
  }

  private onUndone(stop: DeliveryRDto): void {
    this.undoing.set(null);
    this.toast.show('deliveries.driver.undoneToast', { name: stop.customerName });
    this.now.set(Date.now());
    this.round.reload();
  }

  /** Délai dépassé, stock plus disponible… : le serveur refuse, on relit la tournée pour montrer l'état réel. */
  private onRefused(stop: DeliveryRDto, error: unknown): void {
    this.undoing.set(null);
    this.toast.show(
      'deliveries.driver.undoRefusedToast',
      { name: stop.customerName, message: errorMessage(error) },
      'danger',
    );
    this.round.reload();
  }
}
