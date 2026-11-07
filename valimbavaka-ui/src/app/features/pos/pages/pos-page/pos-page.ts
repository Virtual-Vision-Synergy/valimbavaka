import {
  ChangeDetectionStrategy,
  Component,
  inject,
  linkedSignal,
  signal,
  viewChild,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { tap } from 'rxjs';
import { TranslatePipe } from '@ngx-translate/core';
import { errorMessage } from '../../../../core/http/problem-detail';
import { SubmitResult } from '../../../../core/offline/outbox-models';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { formatAriary } from '../../../../shared/pipes/ariary-pipe';
import { PosCatalog } from '../../components/pos-catalog/pos-catalog';
import { PosTicket } from '../../components/pos-ticket/pos-ticket';
import { PosCart } from '../../data-access/pos-cart';
import { toSaleWDto } from '../../data-access/pos-mapping';
import { PosArticleRDto, SaleRDto } from '../../data-access/pos-models';
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
      this.posService.context().pipe(
        tap((ctx) => {
          this.cart.start(ctx.articles, ctx.promotion, ctx.openTicket, ctx.openTicketReceived);
          this.cart.seller.set(ctx.defaultSellerId);
        }),
      ),
  });
  protected readonly ticketNumber = linkedSignal(() => this.context.value()?.ticketNumber ?? 0);
  protected readonly heldCount = linkedSignal(() => this.context.value()?.heldTickets ?? 0);
  /** Vrai pendant l'envoi : un second F12 n'enregistre pas la vente deux fois. */
  protected readonly submitting = signal(false);

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

  /** Enregistre la vente (VTE-01) : canal, vendeuse, règlement et lignes ; le serveur fixe prix et numéro. */
  protected checkout(): void {
    const sellerId = this.cart.seller();
    const channelId = this.context.value()?.channelIds[this.cart.channel()];
    if (this.submitting()) {
      return;
    }
    if (!sellerId || !channelId || !this.cart.canCheckout()) {
      this.toast.show(
        sellerId ? 'pos.toast.cannotCheckout' : 'pos.toast.sellerRequired',
        {},
        'warning',
      );
      return;
    }
    this.submitting.set(true);
    const body = toSaleWDto(channelId, sellerId, this.cart.payment(), this.cart.priced());
    this.posService.record(body).subscribe({
      next: (result) => this.recorded(result),
      error: (error: unknown) => {
        this.submitting.set(false);
        this.toast.show('pos.toast.failed', { message: errorMessage(error) }, 'danger');
      },
    });
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

  private recorded(result: SubmitResult<SaleRDto>): void {
    this.submitting.set(false);
    if (result.queued || !result.body) {
      this.toast.show('common.queuedSaved', {}, 'info');
    } else {
      this.toast.show('pos.toast.paid', {
        ticket: result.body.number,
        amount: formatAriary(result.body.total),
      });
    }
    this.ticketNumber.update((number) => number + 1);
    this.cart.clear();
  }
}
