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
import { RouterLink } from '@angular/router';
import { Lock, ScanBarcode } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language-service';
import { ChipOption, FilterChips } from '../../../../shared/components/filter-chips/filter-chips';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { errorMessage } from '../../../../core/http/problem-detail';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ShortDatePipe } from '../../../../shared/pipes/short-date-pipe';
import { InventoryFilter, InventoryState } from '../../data-access/inventory-state';
import {
  INVENTORY_REASONS,
  InventoryReason,
  InventorySheetLine,
} from '../../data-access/inventory-models';
import { parseQuantity, toLocalMinute } from '../../data-access/inventory-sheet';
import { InventoryService } from '../../data-access/inventory-service';

/**
 * Session d'inventaire en cours (INV-01 à INV-05) : comptage article par article enregistré sur le serveur
 * (INV-02), écart calculé en direct, motif de chaque écart enregistré sur le serveur (INV-03), clôture quand tout
 * est compté et expliqué : le serveur ajuste alors le stock aux quantités comptées (INV-04).
 */
@Component({
  selector: 'vvs-inventory-page',
  imports: [
    RouterLink,
    TranslatePipe,
    AriaryPipe,
    ShortDatePipe,
    Icon,
    ListState,
    FilterChips,
    StatusBadge,
  ],
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
    effect(() => this.state.session.set(this.inventory.value()?.session ?? undefined));
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

  /** Quantité validée (sortie du champ, Entrée) : enregistrée ; le théorique figé par le serveur est repris. */
  protected saveCount(line: InventorySheetLine, raw: string): void {
    const sessionId = this.inventory.value()?.session?.id;
    const countedQuantity = parseQuantity(raw);
    if (!sessionId || countedQuantity === null) {
      return;
    }
    this.inventoryService.count(sessionId, line.lineId, { countedQuantity }).subscribe({
      next: (result) => {
        if (result.body) {
          this.state.applyCount(line.articleId, result.body.theoreticalQuantity);
          this.state.setReason(line.articleId, result.body.reason ?? '');
        }
      },
      error: (error: unknown) =>
        this.toast.show(
          'inventory.countFailed',
          { article: line.reference, reason: errorMessage(error) },
          'danger',
        ),
    });
  }

  /**
   * Motif choisi : affiché tout de suite, puis enregistré (INV-03). Revenir sur « Choisir un motif… »
   * n'envoie rien : le serveur garde le dernier motif, qui revient au rechargement.
   */
  protected saveReason(line: InventorySheetLine, raw: string): void {
    this.state.setReason(line.articleId, raw);
    const sessionId = this.inventory.value()?.session?.id;
    if (!sessionId || !raw) {
      return;
    }
    const reason = raw as InventoryReason;
    this.inventoryService.qualify(sessionId, line.lineId, { reason }).subscribe({
      error: (error: unknown) =>
        this.toast.show(
          'inventory.reasonFailed',
          { article: line.reference, reason: errorMessage(error) },
          'danger',
        ),
    });
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

  /**
   * Clôture confirmée (INV-04) : la feuille est verrouillée à l'heure de clôture du serveur. Hors ligne, la clôture
   * attend en file et la feuille reste ouverte ; un refus du serveur (ligne non comptée, écart sans motif) est
   * affiché.
   */
  protected confirmClose(): void {
    const session = this.inventory.value()?.session;
    if (!session) {
      return;
    }
    this.closing.set(true);
    this.inventoryService.close(session.id).subscribe({
      next: (result) => {
        this.closing.set(false);
        this.cancelClose();
        if (!result.body) {
          this.toast.show('common.queuedSaved', {}, 'info');
          return;
        }
        const closedAt = result.body.closedAt ?? new Date().toISOString();
        this.closedAt.set(toLocalMinute(closedAt).slice(11));
        this.toast.show('inventory.closedToast', { number: result.body.number });
      },
      error: (error: unknown) => {
        this.closing.set(false);
        this.cancelClose();
        this.toast.show(
          'inventory.closeFailed',
          { number: session.number, reason: errorMessage(error) },
          'danger',
        );
      },
    });
  }
}
