import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { OutboxService } from '../../../../core/offline/outbox-service';

/** Saisies refusées par le serveur lors de la synchronisation : l'utilisateur les renvoie ou les abandonne. */
@Component({
  selector: 'vvs-sync-page',
  imports: [TranslatePipe],
  templateUrl: './sync-page.html',
  styleUrl: './sync-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SyncPage {
  private readonly outbox = inject(OutboxService);

  protected readonly failed = this.outbox.failed;
  protected readonly pendingCount = this.outbox.pendingCount;
  protected readonly isSyncing = this.outbox.isSyncing;

  protected retry(id: string): void {
    void this.outbox.retry(id);
  }

  protected discard(id: string): void {
    void this.outbox.discard(id);
  }
}
