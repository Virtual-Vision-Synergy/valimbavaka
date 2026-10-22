import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  computed,
  inject,
  linkedSignal,
  signal,
  viewChild,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CircleCheck, CircleX, CloudUpload, Navigation, Phone, Wifi, WifiOff } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthStore } from '../../../../core/auth/auth-store';
import { LanguageService } from '../../../../core/i18n/language-service';
import { ConnectivityService } from '../../../../core/offline/connectivity-service';
import { OutboxService } from '../../../../core/offline/outbox-service';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { mapsHref, telHref } from '../../data-access/delivery-links';
import { DeliveryRDto, DeliveryStatusWDto, FailureReason } from '../../data-access/delivery-models';
import { DeliveryService } from '../../data-access/delivery-service';

/** Délai pendant lequel la dernière saisie peut être annulée avant envoi. */
export const UNDO_DELAY_MS = 5000;

const FAILURE_REASONS: readonly FailureReason[] = [
  'ABSENT',
  'ADDRESS_NOT_FOUND',
  'REFUSED',
  'OTHER',
];

interface PendingChange {
  readonly delivery: DeliveryRDto;
  readonly change: DeliveryStatusWDto;
  readonly previous: readonly DeliveryRDto[];
  readonly timer: ReturnType<typeof setTimeout>;
}

/**
 * Tournée du livreur (LIV-02, LIV-08), pensée pour une main, dehors et parfois hors ligne :
 * un seul arrêt mis en avant, deux grosses actions, annulation possible pendant 5 s.
 */
@Component({
  selector: 'vvs-driver-round-page',
  imports: [ReactiveFormsModule, TranslatePipe, AriaryPipe, Icon, ListState],
  templateUrl: './driver-round-page.html',
  styleUrl: './driver-round-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DriverRoundPage {
  private readonly deliveryService = inject(DeliveryService);
  private readonly authStore = inject(AuthStore);
  private readonly language = inject(LanguageService);
  private readonly connectivity = inject(ConnectivityService);
  private readonly failureDialog = viewChild<ElementRef<HTMLDialogElement>>('failureDialog');

  protected readonly icons = {
    CircleCheck,
    CircleX,
    CloudUpload,
    Navigation,
    Phone,
    Wifi,
    WifiOff,
  };
  protected readonly reasons = FAILURE_REASONS;
  protected readonly telHref = telHref;
  protected readonly mapsHref = mapsHref;
  protected readonly online = this.connectivity.isOnline;
  protected readonly pendingSync = inject(OutboxService).pendingCount;

  protected readonly round = rxResource({ stream: () => this.deliveryService.myRound() });
  protected readonly stops = linkedSignal<readonly DeliveryRDto[]>(
    () => this.round.value()?.stops ?? [],
  );
  protected readonly pending = signal<PendingChange | null>(null);
  protected readonly failing = signal<DeliveryRDto | null>(null);
  protected readonly failureForm = inject(FormBuilder).nonNullable.group({
    reason: ['ABSENT' as FailureReason],
    note: ['', Validators.maxLength(280)],
  });

  protected readonly firstName = computed(() => this.authStore.currentUser()?.firstName ?? '');
  protected readonly longDate = computed(() =>
    new Intl.DateTimeFormat(this.language.current(), {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
    }).format(new Date(`${this.round.value()?.date ?? ''}T12:00:00`)),
  );
  protected readonly upcoming = computed(() =>
    this.stops().filter((stop) => stop.status === 'ON_ROUND'),
  );
  protected readonly next = computed(() => this.upcoming()[0]);
  protected readonly doneCount = computed(() => this.stops().length - this.upcoming().length);
  protected readonly collected = computed(() =>
    this.sumToCollect(this.stops().filter((stop) => stop.status === 'DELIVERED')),
  );
  protected readonly remaining = computed(() => this.sumToCollect(this.upcoming()));

  constructor() {
    inject(DestroyRef).onDestroy(() => this.commit());
  }

  protected markDelivered(delivery: DeliveryRDto): void {
    this.apply(delivery, { status: 'DELIVERED', failureReason: null, note: null });
  }

  protected openFailure(delivery: DeliveryRDto): void {
    this.failing.set(delivery);
    this.failureForm.reset();
    this.failureDialog()?.nativeElement.showModal();
  }

  protected closeFailure(): void {
    this.failureDialog()?.nativeElement.close();
  }

  protected confirmFailure(): void {
    const delivery = this.failing();
    if (!delivery || this.failureForm.invalid) {
      return;
    }
    const { reason, note } = this.failureForm.getRawValue();
    this.apply(delivery, { status: 'FAILED', failureReason: reason, note: note.trim() || null });
    this.closeFailure();
  }

  protected undo(): void {
    const pending = this.pending();
    if (pending) {
      clearTimeout(pending.timer);
      this.stops.set(pending.previous);
      this.pending.set(null);
    }
  }

  private apply(delivery: DeliveryRDto, change: DeliveryStatusWDto): void {
    this.commit();
    const previous = this.stops();
    this.stops.update((stops) =>
      stops.map((stop) =>
        stop.id === delivery.id
          ? { ...stop, status: change.status, failureReason: change.failureReason }
          : stop,
      ),
    );
    const timer = setTimeout(() => this.commit(), UNDO_DELAY_MS);
    this.pending.set({ delivery, change, previous, timer });
  }

  /** Envoie la saisie en attente d'annulation (au délai, à la saisie suivante ou en quittant). */
  private commit(): void {
    const pending = this.pending();
    if (pending) {
      clearTimeout(pending.timer);
      this.pending.set(null);
      this.deliveryService.updateStatus(pending.delivery.id, pending.change).subscribe();
    }
  }

  private sumToCollect(stops: readonly DeliveryRDto[]): number {
    return stops.filter((stop) => !stop.prepaid).reduce((sum, stop) => sum + stop.amount, 0);
  }
}
