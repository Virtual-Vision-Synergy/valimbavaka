import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  afterNextRender,
  computed,
  effect,
  inject,
  Injector,
  signal,
  viewChild,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { rxResource } from '@angular/core/rxjs-interop';
import { Lock, LockKeyhole } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { ApiError, errorMessage } from '../../../../core/http/problem-detail';
import { SubmitResult } from '../../../../core/offline/outbox-models';
import { LanguageService } from '../../../../core/i18n/language-service';
import { ChipOption, FilterChips } from '../../../../shared/components/filter-chips/filter-chips';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import {
  ChecklistAction,
  ClosingChecklist,
} from '../../components/closing-checklist/closing-checklist';
import { ClosingState } from '../../data-access/closing-state';
import {
  DayClosing,
  DISCREPANCY_REASONS,
  DISPATCH_DESTINATIONS,
  DispatchDestination,
} from '../../data-access/day-models';
import { DayService } from '../../data-access/day-service';

/** Statut HTTP d'une erreur de lecture (`ApiError` de l'intercepteur, ou réponse brute éventuellement enveloppée). */
function statusOf(error: unknown): number | undefined {
  if (error instanceof ApiError || error instanceof HttpErrorResponse) {
    return error.status;
  }
  return error instanceof Error && error.cause ? statusOf(error.cause) : undefined;
}

/**
 * Clôture de journée (JR-04 à JR-06) : réconciliation par emplacement, motifs d'écart,
 * caisse, puis verrouillage — possible seulement quand tous les points sont verts.
 */
@Component({
  selector: 'vvs-day-close-page',
  imports: [TranslatePipe, AriaryPipe, Icon, PageHeader, ListState, FilterChips, ClosingChecklist],
  templateUrl: './day-close-page.html',
  styleUrl: './day-close-page.css',
  providers: [ClosingState],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DayClosePage {
  private readonly dayService = inject(DayService);
  private readonly toast = inject(ToastService);
  private readonly language = inject(LanguageService);
  private readonly injector = inject(Injector);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly lockDialog = viewChild<ElementRef<HTMLDialogElement>>('lockDialog');

  protected readonly state = inject(ClosingState);
  protected readonly icons = { Lock, LockKeyhole };
  protected readonly reasons = DISCREPANCY_REASONS;
  protected readonly closing = rxResource({ stream: () => this.dayService.closing() });
  /** Le serveur refuse la réconciliation (409) tant que la journée du jour n'est pas ouverte (JR-01). */
  protected readonly notOpened = computed(() => statusOf(this.closing.error()) === 409);
  /** Réconciliation lue ; `undefined` tant qu'elle charge ou si la lecture a échoué (`value()` lève en erreur). */
  protected readonly data = computed(() =>
    this.closing.hasValue() ? this.closing.value() : undefined,
  );
  protected readonly location = signal<DispatchDestination>('SHOP');
  protected readonly onlyGaps = signal(false);
  /** Heure du verrouillage fait depuis cet écran ; absente quand la journée était déjà clôturée au chargement. */
  protected readonly lockedAt = signal<string | null>(null);
  protected readonly locking = signal(false);
  protected readonly lockError = signal<string | null>(null);
  /** Journée clôturée (JR-06) : consultation seule. */
  protected readonly locked = computed(() => this.data()?.status === 'CLOSED');
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);

  protected readonly locationOptions = computed<readonly ChipOption<DispatchDestination>[]>(() =>
    DISPATCH_DESTINATIONS.map((value) => ({
      value,
      labelKey: `day.destination.${value}`,
      count: this.state.rows().filter((row) => row.location === value && row.gap !== 0).length,
    })),
  );
  protected readonly locationRows = computed(() =>
    this.state.rows().filter((row) => row.location === this.location()),
  );
  protected readonly visibleRows = computed(() =>
    this.locationRows().filter((row) => !this.onlyGaps() || row.gap !== 0),
  );
  protected readonly cleanCount = computed(
    () => this.locationRows().filter((row) => row.gap === 0).length,
  );
  protected readonly cash = computed(() => {
    const location = this.closing
      .value()
      ?.locations.find((item) => item.location === this.location());
    const counted = this.state.cash()[this.location()] ?? 0;
    return location ? { ...location.cash, counted, gap: counted - location.cash.expected } : null;
  });
  /**
   * Verrouillage possible : chaque écart a son motif et le comptage affiché est celui que le serveur a enregistré
   * (il le relit). La caisse et la liste de contrôle, encore en démonstration, ne bloquent pas.
   */
  protected readonly canLock = computed(
    () =>
      !!this.data() &&
      !this.locked() &&
      !this.locking() &&
      this.state.missingReasons() === 0 &&
      this.state.unsaved() === 0,
  );
  /** Comptage enregistrable : au moins une ligne, un motif pour chaque écart (le serveur le vérifie aussi). */
  protected readonly canSave = computed(
    () =>
      !this.saving() &&
      !this.locked() &&
      this.state.rows().length > 0 &&
      this.state.missingReasons() === 0,
  );
  protected readonly title = computed(() => {
    const date = this.data()?.date;
    const label = date
      ? new Intl.DateTimeFormat(this.language.current(), {
          weekday: 'long',
          day: 'numeric',
          month: 'long',
        }).format(new Date(`${date}T12:00:00`))
      : '';
    return label;
  });

  constructor() {
    effect(() => this.state.data.set(this.data()));
  }

  protected onChecklist(action: ChecklistAction): void {
    if (action === 'reintegrate') {
      this.state.lateShootings.set(0);
      this.toast.show('day.close.reintegrated');
      return;
    }
    const missing = this.state.rows().find((row) => row.gap !== 0 && !row.reason);
    if (missing) {
      this.location.set(missing.location);
      this.onlyGaps.set(true);
      afterNextRender(
        () =>
          this.host.nativeElement
            .querySelector<HTMLSelectElement>(`[id="reason-${missing.key}"]`)
            ?.focus(),
        { injector: this.injector },
      );
    }
  }

  /** Enregistre le comptage et les motifs ; le serveur renvoie 422 pour tout écart sans motif. */
  protected saveCounts(): void {
    if (!this.canSave()) {
      return;
    }
    this.saving.set(true);
    this.saveError.set(null);
    this.dayService.saveCounts(this.state.toCountsRequest()).subscribe({
      next: (result) => this.saved(result),
      error: (error: unknown) => {
        this.saving.set(false);
        this.saveError.set(errorMessage(error));
      },
    });
  }

  private saved(result: SubmitResult<DayClosing>): void {
    this.saving.set(false);
    if (result.queued || !result.body) {
      this.toast.show('common.queuedSaved', {}, 'info');
      return;
    }
    this.closing.set(result.body);
    this.toast.show('day.close.count.saved', { count: this.state.rows().length });
  }

  protected openLock(): void {
    this.lockDialog()?.nativeElement.showModal();
  }

  protected closeLock(): void {
    this.lockDialog()?.nativeElement.close();
  }

  /** Verrouille la journée : le serveur relit les écarts et refuse (422) un comptage incomplet ou sans motif. */
  protected confirmLock(): void {
    if (!this.canLock()) {
      return;
    }
    this.locking.set(true);
    this.lockError.set(null);
    this.dayService.lock().subscribe({
      next: (result) => this.lockDone(result),
      error: (error: unknown) => {
        this.locking.set(false);
        this.closeLock();
        this.lockError.set(errorMessage(error));
      },
    });
  }

  private lockDone(result: SubmitResult<DayClosing>): void {
    this.locking.set(false);
    this.closeLock();
    if (result.queued || !result.body) {
      this.toast.show('common.queuedSaved', {}, 'info');
      return;
    }
    const time = new Date().toTimeString().slice(0, 5);
    this.lockedAt.set(time);
    this.closing.set(result.body);
    this.toast.show('day.close.lockedToast', { time });
  }
}
