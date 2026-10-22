import { LowerCasePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  inject,
  linkedSignal,
  signal,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { CircleCheck, RotateCcw, TriangleAlert, Undo2 } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language-service';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { StepItem, Stepper } from '../../../../shared/components/stepper/stepper';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import {
  DISPATCH_DESTINATIONS,
  DispatchDestination,
  DispatchQuantities,
  DispatchVoucherRDto,
  sumQuantities,
} from '../../data-access/day-models';
import { DayService } from '../../data-access/day-service';

type QuantityMap = Readonly<Record<string, DispatchQuantities>>;

/**
 * Répartition du matin (JR-01 à JR-03) : on sort le stock de l'entrepôt vers chaque point de vente
 * en un seul écran, au clavier (Entrée descend d'une ligne), sans jamais dépasser l'entrepôt.
 */
@Component({
  selector: 'vvs-day-open-page',
  imports: [LowerCasePipe, RouterLink, TranslatePipe, Icon, PageHeader, ListState, Stepper],
  templateUrl: './day-open-page.html',
  styleUrl: './day-open-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DayOpenPage {
  private readonly dayService = inject(DayService);
  private readonly toast = inject(ToastService);
  private readonly language = inject(LanguageService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly icons = { CircleCheck, RotateCcw, TriangleAlert, Undo2 };
  protected readonly destinations = DISPATCH_DESTINATIONS;
  protected readonly opening = rxResource({ stream: () => this.dayService.opening() });
  protected readonly quantities = linkedSignal<QuantityMap>(() =>
    Object.fromEntries(
      (this.opening.value()?.lines ?? []).map((line) => [line.articleId, line.proposed]),
    ),
  );
  protected readonly vouchers = signal<readonly DispatchVoucherRDto[] | null>(null);
  protected readonly submitting = signal(false);

  protected readonly rows = computed(() =>
    (this.opening.value()?.lines ?? []).map((line) => {
      const quantities = this.quantities()[line.articleId] ?? line.proposed;
      const remaining = line.warehouse - sumQuantities(quantities);
      const level = remaining < 0 ? 'error' : remaining <= line.threshold / 4 ? 'low' : 'ok';
      return { line, quantities, remaining, level };
    }),
  );
  protected readonly conflicts = computed(() => this.rows().filter((row) => row.level === 'error'));
  protected readonly summary = computed(() =>
    DISPATCH_DESTINATIONS.map((destination) => ({
      destination,
      units: this.rows().reduce((sum, row) => sum + row.quantities[destination], 0),
      lines: this.rows().filter((row) => row.quantities[destination] > 0).length,
    })),
  );
  protected readonly voucherCount = computed(
    () => this.summary().filter((destination) => destination.units > 0).length,
  );
  protected readonly steps = computed<readonly StepItem[]>(() => {
    const done = this.vouchers() !== null;
    return [
      {
        labelKey: 'day.open.step.carryOver',
        captionKey: 'day.open.step.carryOverHint',
        params: { day: this.weekday(this.opening.value()?.previousDay) },
        state: 'done',
      },
      {
        labelKey: 'day.open.step.dispatch',
        captionKey: 'day.open.step.dispatchHint',
        state: done ? 'done' : 'current',
      },
      {
        labelKey: 'day.open.step.validate',
        captionKey: 'day.open.step.validateHint',
        state: done ? 'done' : 'upcoming',
      },
    ];
  });
  protected readonly subtitle = computed(() => {
    const date = this.opening.value()?.date;
    const label = date ? this.format(date, { weekday: 'long', day: 'numeric', month: 'long' }) : '';
    return label.charAt(0).toUpperCase() + label.slice(1);
  });
  protected readonly weekdayName = computed(() => this.weekday(this.opening.value()?.date));

  protected setQuantity(articleId: string, destination: DispatchDestination, raw: string): void {
    const value = Math.max(0, Math.floor(Number(raw) || 0));
    this.quantities.update((all) => ({
      ...all,
      [articleId]: { ...all[articleId], [destination]: value },
    }));
  }

  protected useYesterday(): void {
    const lines = this.opening.value()?.lines ?? [];
    this.quantities.set(Object.fromEntries(lines.map((line) => [line.articleId, line.yesterday])));
  }

  protected reset(): void {
    const lines = this.opening.value()?.lines ?? [];
    const zero: DispatchQuantities = { SHOP: 0, DELIVERY: 0, SALON: 0 };
    this.quantities.set(Object.fromEntries(lines.map((line) => [line.articleId, zero])));
  }

  /** Entrée : passe à la même colonne de la ligne suivante, comme dans un tableur. */
  protected moveDown(event: Event, index: number, destination: DispatchDestination): void {
    event.preventDefault();
    this.focusCell(index + 1, destination);
  }

  protected focusRow(articleId: string): void {
    const index = this.rows().findIndex((row) => row.line.articleId === articleId);
    this.focusCell(index, 'SHOP');
  }

  protected validate(): void {
    const conflict = this.conflicts()[0];
    const opening = this.opening.value();
    if (conflict || !opening) {
      this.focusRow(conflict?.line.articleId ?? '');
      return;
    }
    this.submitting.set(true);
    const lines = this.rows().map((row) => ({
      articleId: row.line.articleId,
      quantities: row.quantities,
    }));
    this.dayService.dispatch({ date: opening.date, lines }).subscribe((vouchers) => {
      this.submitting.set(false);
      this.vouchers.set(vouchers);
      this.toast.show('day.open.validated', { count: vouchers.length });
    });
  }

  private focusCell(index: number, destination: DispatchDestination): void {
    const cell = this.host.nativeElement.querySelector<HTMLInputElement>(
      `#qty-${destination}-${index}`,
    );
    cell?.focus();
    cell?.select();
  }

  private weekday(date: string | undefined): string {
    return date ? this.format(date, { weekday: 'long' }) : '';
  }

  private format(date: string, options: Intl.DateTimeFormatOptions): string {
    return new Intl.DateTimeFormat(this.language.current(), options).format(
      new Date(`${date}T12:00:00`),
    );
  }
}
