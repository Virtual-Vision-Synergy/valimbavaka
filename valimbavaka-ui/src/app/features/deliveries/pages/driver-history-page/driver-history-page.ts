import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { DeliveryService } from '../../data-access/delivery-service';
import { DELIVERY_STATUS_TONES } from '../../data-access/delivery-tones';

/** Historique des arrêts terminés de la journée, du plus récent au plus ancien (LIV-02). */
@Component({
  selector: 'vvs-driver-history-page',
  imports: [TranslatePipe, AriaryPipe, ListState, StatusBadge],
  templateUrl: './driver-history-page.html',
  styleUrl: './driver-history-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DriverHistoryPage {
  protected readonly round = rxResource({ stream: () => inject(DeliveryService).myRound() });
  protected readonly tones = DELIVERY_STATUS_TONES;
  protected readonly done = computed(() =>
    (this.round.value()?.stops ?? [])
      .filter((stop) => stop.status === 'DELIVERED' || stop.status === 'FAILED')
      .sort((a, b) => b.time.localeCompare(a.time)),
  );
}
