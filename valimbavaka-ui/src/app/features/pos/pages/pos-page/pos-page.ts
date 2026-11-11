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
import { ShortcutAction, shortcutAction } from '../../data-access/pos-shortcuts';

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
    const action = shortcutAction(event);
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
    const displayedTotal = this.cart.total();
    this.posService.record(body).subscribe({
      next: (result) => this.recorded(result, displayedTotal),
      error: (error: unknown) => {
        this.submitting.set(false);
        this.toast.show('pos.toast.failed', { message: errorMessage(error) }, 'danger');
      },
    });
  }

  /**
   * Met le ticket en attente (VTE-06) : aucune vente n'est enregistrée, le panier est vidé et le
   * numéro suivant est proposé. Refusé pendant l'envoi d'un encaissement (le panier part avec la vente).
   */
  protected hold(): void {
    if (this.submitting()) {
      return;
    }
    if (this.cart.isEmpty()) {
      this.toast.show('pos.toast.holdEmpty', {}, 'warning');
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
      increase: () => (last ? this.cart.add(last) : this.catalog()?.focusSearch()),
      decrease: () => last && this.cart.decrement(last.id),
    };
    handlers[action]();
  }

  /**
   * Le serveur recalcule la vente avec les prix du moment (VTE-01) : si son net diffère du net affiché
   * (prix modifié depuis l'ouverture du poste), la caissière est avertie pour ajuster le rendu (VTE-02).
   */
  private recorded(result: SubmitResult<SaleRDto>, displayedTotal: number): void {
    this.submitting.set(false);
    const sale = result.body;
    if (result.queued || !sale) {
      this.toast.show('common.queuedSaved', {}, 'info');
    } else if (sale.total !== displayedTotal) {
      this.toast.show(
        'pos.toast.totalAdjusted',
        {
          ticket: sale.number,
          amount: formatAriary(sale.total),
          displayed: formatAriary(displayedTotal),
        },
        'warning',
      );
    } else {
      this.toast.show('pos.toast.paid', { ticket: sale.number, amount: formatAriary(sale.total) });
    }
    this.ticketNumber.update((number) => number + 1);
    this.cart.clear();
  }
}
