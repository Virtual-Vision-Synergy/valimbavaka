import { ChangeDetectionStrategy, Component, inject, linkedSignal, viewChild } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { tap } from 'rxjs';
import { TranslatePipe } from '@ngx-translate/core';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { formatAriary } from '../../../../shared/pipes/ariary-pipe';
import { PosCatalog } from '../../components/pos-catalog/pos-catalog';
import { PosTicket } from '../../components/pos-ticket/pos-ticket';
import { PosCart } from '../../data-access/pos-cart';
import { PosArticleRDto } from '../../data-access/pos-models';
import { PosService } from '../../data-access/pos-service';

type ShortcutAction = 'search' | 'promo' | 'hold' | 'checkout' | 'increase' | 'decrease';

const FUNCTION_KEYS: Readonly<Record<string, ShortcutAction>> = {
  F2: 'search',
  F6: 'promo',
  F8: 'hold',
  F12: 'checkout',
};

const QUANTITY_KEYS: Readonly<Record<string, ShortcutAction>> = {
  '+': 'increase',
  '-': 'decrease',
};

/** Vrai quand la frappe vise un champ de saisie : les touches + / − y restent du texte. */
function isTyping(event: KeyboardEvent): boolean {
  const target = event.target as HTMLElement | null;
  return !!target && (target.isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test(target.tagName));
}

/**
 * Poste de caisse (VTE-02 à VTE-06), desktop et tablette : un seul écran qui s'adapte au pointeur
 * (pavé numérique et cibles 44 px au doigt, raccourcis F2/F6/F8/F12 au clavier).
 */
@Component({
  selector: 'vvs-pos-page',
  imports: [TranslatePipe, PageHeader, ListState, PosCatalog, PosTicket],
  providers: [PosCart],
  templateUrl: './pos-page.html',
  styleUrl: './pos-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown)': 'onKeydown($event)' },
})
export class PosPage {
  private readonly posService = inject(PosService);
  private readonly toast = inject(ToastService);
  protected readonly cart = inject(PosCart);
  private readonly catalog = viewChild(PosCatalog);

  protected readonly context = rxResource({
    stream: () =>
      this.posService
        .context()
        .pipe(
          tap((ctx) =>
            this.cart.start(ctx.articles, ctx.promotion, ctx.openTicket, ctx.openTicketReceived),
          ),
        ),
  });
  protected readonly ticketNumber = linkedSignal(() => this.context.value()?.ticketNumber ?? 0);
  protected readonly heldCount = linkedSignal(() => this.context.value()?.heldTickets ?? 0);

  protected onKeydown(event: KeyboardEvent): void {
    const action =
      FUNCTION_KEYS[event.key] ?? (isTyping(event) ? undefined : QUANTITY_KEYS[event.key]);
    if (!action || !this.context.value()) {
      return;
    }
    event.preventDefault();
    this.run(action);
  }

  protected pick(article: PosArticleRDto): void {
    this.cart.add(article);
  }

  protected checkout(): void {
    if (!this.cart.canCheckout()) {
      this.toast.show('pos.toast.cannotCheckout', {}, 'warning');
      return;
    }
    this.toast.show('pos.toast.paid', {
      ticket: `VTE-${this.ticketNumber()}`,
      amount: formatAriary(this.cart.total()),
    });
    this.ticketNumber.update((number) => number + 1);
    this.cart.clear();
  }

  protected hold(): void {
    if (this.cart.isEmpty()) {
      return;
    }
    this.toast.show('pos.toast.held', { ticket: `VTE-${this.ticketNumber()}` }, 'info');
    this.heldCount.update((count) => count + 1);
    this.ticketNumber.update((number) => number + 1);
    this.cart.clear();
  }

  protected demoAction(): void {
    this.toast.show('common.demoAction', {}, 'info');
  }

  private run(action: ShortcutAction): void {
    const last = this.cart.lastArticle();
    const handlers: Record<ShortcutAction, () => void> = {
      search: () => this.catalog()?.focusSearch(),
      promo: () => this.demoAction(),
      hold: () => this.hold(),
      checkout: () => this.checkout(),
      increase: () => last && this.cart.add(last),
      decrease: () => last && this.cart.decrement(last.id),
    };
    handlers[action]();
  }
}
