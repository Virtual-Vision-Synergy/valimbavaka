import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language-service';
import { ConnectivityService } from '../../../../core/offline/connectivity-service';
import { OutboxEntry } from '../../../../core/offline/outbox-models';
import { OutboxService } from '../../../../core/offline/outbox-service';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../../shared/components/toast/toast-service';

/**
 * Suivi des saisies du compte (SYS-09) : celles en attente d'envoi (lecture seule, dans l'ordre de la file)
 * et celles refusées, avec leur motif ; une saisie refusée se renvoie, ou s'abandonne après confirmation.
 */
@Component({
  selector: 'vvs-sync-page',
  imports: [TranslatePipe, ConfirmDialog, StatusBadge],
  templateUrl: './sync-page.html',
  styleUrl: './sync-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SyncPage {
  private readonly outbox = inject(OutboxService);
  private readonly toast = inject(ToastService);
  private readonly language = inject(LanguageService);

  protected readonly failed = this.outbox.failed;
  protected readonly pending = this.outbox.pending;
  protected readonly isSyncing = this.outbox.isSyncing;
  protected readonly isOnline = inject(ConnectivityService).isOnline;
  /** Saisie dont l'abandon attend la confirmation de l'utilisateur. */
  protected readonly toDiscard = signal<OutboxEntry | null>(null);

  protected async retry(entry: OutboxEntry): Promise<void> {
    const offline = !this.isOnline();
    await this.outbox.retry(entry.id);
    this.toast.show(offline ? 'sync.page.retriedOffline' : 'sync.page.retried', {}, 'info');
  }

  protected askDiscard(entry: OutboxEntry): void {
    this.toDiscard.set(entry);
  }

  protected cancelDiscard(): void {
    this.toDiscard.set(null);
  }

  protected async confirmDiscard(): Promise<void> {
    const entry = this.toDiscard();
    this.toDiscard.set(null);
    if (entry) {
      await this.outbox.discard(entry.id);
      this.toast.show('sync.page.discarded');
    }
  }

  /** Date et heure de la saisie, au format de la langue choisie. */
  protected savedAt(entry: OutboxEntry): string {
    return new Intl.DateTimeFormat(this.language.current(), {
      dateStyle: 'short',
      timeStyle: 'short',
    }).format(new Date(entry.createdAt));
  }
}
