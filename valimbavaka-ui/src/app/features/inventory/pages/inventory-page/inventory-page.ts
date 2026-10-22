import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Lock, ScanBarcode } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language-service';
import { ChipOption, FilterChips } from '../../../../shared/components/filter-chips/filter-chips';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ShortDatePipe } from '../../../../shared/pipes/short-date-pipe';
import { InventoryFilter, InventoryState } from '../../data-access/inventory-state';
import { INVENTORY_REASONS } from '../../data-access/inventory-models';
import { InventoryService } from '../../data-access/inventory-service';

/**
 * Session d'inventaire (INV-01 à INV-05) : comptage article par article, écart calculé en direct,
 * motif obligatoire pour chaque écart, clôture seulement quand tout est compté et expliqué.
 */
@Component({
  selector: 'vvs-inventory-page',
  imports: [TranslatePipe, AriaryPipe, ShortDatePipe, Icon, ListState, FilterChips, StatusBadge],
  templateUrl: './inventory-page.html',
  styleUrl: './inventory-page.css',
  providers: [InventoryState],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InventoryPage {
  private readonly inventoryService = inject(InventoryService);
  private readonly toast = inject(ToastService);
  private readonly language = inject(LanguageService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly closeDialog = viewChild<ElementRef<HTMLDialogElement>>('closeDialog');

  protected readonly state = inject(InventoryState);
  protected readonly icons = { Lock, ScanBarcode };
  protected readonly reasons = INVENTORY_REASONS;
  protected readonly inventory = rxResource({ stream: () => this.inventoryService.current() });
  protected readonly closedAt = signal<string | null>(null);
  protected readonly closing = signal(false);
  protected readonly scan = signal('');

  protected readonly filterOptions = computed<readonly ChipOption<InventoryFilter>[]>(() => [
    { value: 'ALL', labelKey: 'inventory.filter.ALL', count: this.state.rows().length },
    {
      value: 'TO_COUNT',
      labelKey: 'inventory.filter.TO_COUNT',
      count: this.state.rows().length - this.state.counted().length,
    },
    { value: 'GAP', labelKey: 'inventory.filter.GAP', count: this.state.gapRows().length },
    {
      value: 'NO_REASON',
      labelKey: 'inventory.filter.NO_REASON',
      count: this.state.missingReasons().length,
    },
  ]);
  protected readonly progress = computed(() => {
    const total = this.state.rows().length;
    return total ? (this.state.counted().length / total) * 100 : 0;
  });
  protected readonly opened = computed(() => {
    const session = this.inventory.value()?.session;
    return session
      ? { date: session.openedAt.split('T')[0], time: session.openedAt.split('T')[1] }
      : null;
  });
  protected readonly numberFormat = computed(() => new Intl.NumberFormat(this.language.current()));

  constructor() {
    effect(() => this.state.session.set(this.inventory.value()?.session));
  }

  /** Code-barres scanné : filtre levé, focus sur le champ de comptage de l'article. */
  protected scanned(barcode: string): void {
    const row = this.state.rows().find((item) => item.line.barcode === barcode.trim());
    this.scan.set('');
    if (!row) {
      this.toast.show('inventory.scanUnknown', { code: barcode }, 'warning');
      return;
    }
    this.state.filter.set('ALL');
    queueMicrotask(() =>
      this.host.nativeElement
        .querySelector<HTMLInputElement>(`[id="count-${row.line.articleId}"]`)
        ?.focus(),
    );
  }

  protected signedGap(gap: number): string {
    return gap > 0 ? `+${gap}` : gap < 0 ? `−${-gap}` : '0';
  }

  protected openClose(): void {
    this.closeDialog()?.nativeElement.showModal();
  }

  protected cancelClose(): void {
    this.closeDialog()?.nativeElement.close();
  }

  protected confirmClose(): void {
    this.closing.set(true);
    this.inventoryService.close(this.state.toRequest()).subscribe(() => {
      this.closing.set(false);
      this.closedAt.set(new Date().toTimeString().slice(0, 5));
      this.cancelClose();
      this.toast.show('inventory.closedToast', {
        number: this.inventory.value()?.session.number ?? '',
      });
    });
  }
}
