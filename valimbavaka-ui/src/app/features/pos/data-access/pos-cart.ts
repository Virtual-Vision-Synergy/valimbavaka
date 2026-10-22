import { Injectable, computed, signal } from '@angular/core';
import {
  PaymentMethod,
  PosArticleRDto,
  PosPromotionRDto,
  PosTicketLineRDto,
  SaleChannel,
} from './pos-models';

export interface CartLine {
  readonly article: PosArticleRDto;
  readonly quantity: number;
}

/** Ligne valorisée : brut, taux et montant de remise (arrondi à l'ariary). */
export interface PricedLine extends CartLine {
  readonly gross: number;
  readonly rate: number;
  readonly discount: number;
}

/** Message à annoncer aux lecteurs d'écran (clé + paramètres traduits à l'affichage). */
export interface CartAnnouncement {
  readonly key: string;
  readonly params: Readonly<Record<string, string | number>>;
}

/**
 * Ticket en cours d'un poste de caisse (état local, fourni par la page) : lignes, canal, paiement
 * et totaux dérivés. Le calcul réel fait foi côté serveur à l'encaissement.
 */
@Injectable()
export class PosCart {
  private readonly lines = signal<readonly CartLine[]>([]);
  private readonly promotion = signal<PosPromotionRDto | null>(null);

  readonly channel = signal<SaleChannel>('SHOP');
  readonly payment = signal<PaymentMethod>('CASH');
  readonly received = signal(0);
  readonly announcement = signal<CartAnnouncement | null>(null);

  readonly priced = computed<readonly PricedLine[]>(() => {
    const promotion = this.promotion();
    return this.lines().map((line) => {
      const gross = line.article.price * line.quantity;
      const rate = promotion?.articleIds.includes(line.article.id) ? promotion.rate : 0;
      return { ...line, gross, rate, discount: Math.round((gross * rate) / 100) };
    });
  });
  readonly quantities = computed(
    () => new Map(this.lines().map((line) => [line.article.id, line.quantity])),
  );
  readonly isEmpty = computed(() => this.lines().length === 0);
  readonly subtotal = computed(() => this.priced().reduce((sum, line) => sum + line.gross, 0));
  readonly discount = computed(() => this.priced().reduce((sum, line) => sum + line.discount, 0));
  readonly total = computed(() => this.subtotal() - this.discount());
  readonly change = computed(() => this.received() - this.total());
  readonly appliedPromotion = computed(() => (this.discount() > 0 ? this.promotion() : null));
  readonly canCheckout = computed(
    () => !this.isEmpty() && (this.payment() !== 'CASH' || this.change() >= 0),
  );

  /** Ouvre le poste : promotion active et éventuel ticket repris. */
  start(
    articles: readonly PosArticleRDto[],
    promotion: PosPromotionRDto | null,
    openTicket: readonly PosTicketLineRDto[],
    received: number,
  ): void {
    const byId = new Map(articles.map((article) => [article.id, article]));
    this.promotion.set(promotion);
    this.received.set(received);
    this.lines.set(
      openTicket.flatMap((line) => {
        const article = byId.get(line.articleId);
        return article ? [{ article, quantity: line.quantity }] : [];
      }),
    );
  }

  /** Ajoute une unité ; refuse au-delà du stock de l'emplacement. */
  add(article: PosArticleRDto): boolean {
    const quantity = this.quantities().get(article.id) ?? 0;
    if (quantity >= article.stock) {
      this.announce('pos.toast.outOfStock', { name: article.name });
      return false;
    }
    this.lines.update((lines) =>
      quantity === 0
        ? [...lines, { article, quantity: 1 }]
        : lines.map((line) =>
            line.article.id === article.id ? { ...line, quantity: line.quantity + 1 } : line,
          ),
    );
    this.announce('pos.ticket.added', { name: article.name, quantity: quantity + 1 });
    return true;
  }

  /** Retire une unité ; la ligne disparaît à zéro. */
  decrement(articleId: string): void {
    const line = this.lines().find((item) => item.article.id === articleId);
    if (!line) {
      return;
    }
    this.lines.update((lines) =>
      line.quantity <= 1
        ? lines.filter((item) => item.article.id !== articleId)
        : lines.map((item) =>
            item.article.id === articleId ? { ...item, quantity: item.quantity - 1 } : item,
          ),
    );
    this.announce(line.quantity <= 1 ? 'pos.ticket.removed' : 'pos.ticket.decreased', {
      name: line.article.name,
      quantity: line.quantity - 1,
    });
  }

  /** Article de la dernière ligne (cible des raccourcis + / −). */
  lastArticle(): PosArticleRDto | undefined {
    return this.lines().at(-1)?.article;
  }

  clear(): void {
    this.lines.set([]);
    this.received.set(0);
    this.payment.set('CASH');
  }

  private announce(key: string, params: Readonly<Record<string, string | number>>): void {
    this.announcement.set({ key, params });
  }
}
