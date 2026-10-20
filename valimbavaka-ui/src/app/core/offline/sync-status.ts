import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ConnectivityService } from './connectivity-service';
import { OutboxService } from './outbox-service';

/** Indicateur discret de l'état du réseau et des saisies en attente de synchronisation (SYS-02). */
@Component({
  selector: 'vvs-sync-status',
  imports: [TranslatePipe],
  templateUrl: './sync-status.html',
  styleUrl: './sync-status.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SyncStatus {
  protected readonly isOnline = inject(ConnectivityService).isOnline;
  private readonly outbox = inject(OutboxService);

  protected readonly pendingCount = this.outbox.pendingCount;
  protected readonly failedCount = this.outbox.failedCount;
}
