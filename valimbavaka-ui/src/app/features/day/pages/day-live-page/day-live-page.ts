import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { RefreshCw } from 'lucide';
import { Subject, catchError, exhaustMap, filter, map, merge, of, timer } from 'rxjs';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { Tone } from '../../../../shared/models/tone';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { DayFeedType, DayLiveRDto } from '../../data-access/day-live-models';
import { DayLiveService } from '../../data-access/day-live-service';

/** Intervalle de relecture : la vue se met à jour seule, sans rechargement manuel. */
export const LIVE_REFRESH_MS = 15_000;

const FEED_TONES: Readonly<Record<string, Tone>> = {
  VALIDATED: 'success',
  CANCELLED: 'danger',
  REVERSED: 'warning',
  TO_PREPARE: 'muted',
  ON_ROUND: 'info',
  DELIVERED: 'success',
  FAILED: 'danger',
};

/**
 * Suivi en direct de la journée (JR-03, administrateur) : ventes, tournée et prestations du jour avec le flux des
 * dernières opérations. La page relit le serveur toutes les 15 s (sans chevauchement, en pause onglet masqué) et garde
 * la dernière vue affichée si une relecture échoue.
 */
@Component({
  selector: 'vvs-day-live-page',
  imports: [TranslatePipe, AriaryPipe, Icon, PageHeader, ListState, StatusBadge],
  templateUrl: './day-live-page.html',
  styleUrl: './day-live-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DayLivePage {
  private readonly service = inject(DayLiveService);
  private readonly manual = new Subject<void>();

  protected readonly icons = { RefreshCw };
  protected readonly live = signal<DayLiveRDto | null>(null);
  /** Vrai quand la dernière relecture a échoué : la vue affichée peut être en retard. */
  protected readonly stale = signal(false);
  protected readonly loading = signal(true);

  constructor() {
    merge(timer(0, LIVE_REFRESH_MS).pipe(filter(() => !document.hidden)), this.manual)
      .pipe(
        exhaustMap(() =>
          this.service.live().pipe(
            map((view) => ({ view, failed: false })),
            catchError(() => of({ view: null, failed: true })),
          ),
        ),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe(({ view, failed }) => {
        this.loading.set(false);
        this.stale.set(failed);
        if (view) {
          this.live.set(view);
        }
      });
  }

  protected refresh(): void {
    this.manual.next();
  }

  protected tone(status: string | null): Tone {
    return (status && FEED_TONES[status]) || 'muted';
  }

  protected feedLabelKey(type: DayFeedType): string {
    return `day.live.feed.${type}`;
  }

  /** Heure locale `HH:MM:SS` d'un instant ISO. */
  protected time(instant: string): string {
    return new Date(instant).toTimeString().slice(0, 8);
  }
}
