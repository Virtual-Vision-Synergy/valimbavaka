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
import { rxResource } from '@angular/core/rxjs-interop';
import { Lock, LockKeyhole } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
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
  DISCREPANCY_REASONS,
  DISPATCH_DESTINATIONS,
  DispatchDestination,
} from '../../data-access/day-models';
import { DayService } from '../../data-access/day-service';

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
  protected readonly location = signal<DispatchDestination>('SHOP');
  protected readonly onlyGaps = signal(false);
  protected readonly lockedAt = signal<string | null>(null);
  protected readonly locking = signal(false);

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
  protected readonly canLock = computed(() => {
    const checklist = this.closing.value()?.checklist;
    return (
      !!checklist &&
      checklist.deliveriesDone === checklist.deliveriesTotal &&
      checklist.heldTickets === 0 &&
      this.state.lateShootings() === 0 &&
      this.state.missingReasons() === 0
    );
  });
  protected readonly title = computed(() => {
    const date = this.closing.value()?.date;
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
    effect(() => this.state.data.set(this.closing.value()));
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

  protected openLock(): void {
    this.lockDialog()?.nativeElement.showModal();
  }

  protected closeLock(): void {
    this.lockDialog()?.nativeElement.close();
  }

  protected confirmLock(): void {
    const date = this.closing.value()?.date;
    if (!date || !this.canLock()) {
      return;
    }
    this.locking.set(true);
    this.dayService.lock(this.state.toRequest(date)).subscribe((time) => {
      this.locking.set(false);
      this.lockedAt.set(time);
      this.closeLock();
      this.toast.show('day.close.lockedToast', { time });
    });
  }
}
