import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Check, WifiOff } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { ConnectivityService } from './connectivity-service';
import { OutboxService } from './outbox-service';
import { Icon } from '../../shared/components/icon/icon';

/** Indicateur discret de l'état du réseau et des saisies en attente de synchronisation (SYS-02). */
@Component({
  selector: 'vvs-sync-status',
  imports: [RouterLink, TranslatePipe, Icon],
  templateUrl: './sync-status.html',
  styleUrl: './sync-status.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SyncStatus {
  protected readonly icons = { Check, WifiOff };
  protected readonly isOnline = inject(ConnectivityService).isOnline;
  private readonly outbox = inject(OutboxService);

  protected readonly pendingCount = this.outbox.pendingCount;
  protected readonly failedCount = this.outbox.failedCount;
}
