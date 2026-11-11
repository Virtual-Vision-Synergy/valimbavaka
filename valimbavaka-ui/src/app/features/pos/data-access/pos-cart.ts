import { Injectable, computed, signal } from '@angular/core';
import { formatAriary } from '../../../shared/pipes/ariary-pipe';
import {
  PaymentMethod,
  PosArticleRDto,
  PosPromotionRDto,
  PosTicketLineRDto,
  SaleChannel,
} from './pos-models';
import { bestPromotion, changeDue, lineTotal, ticketTotals } from './pos-pricing';

export interface CartLine {
  readonly article: PosArticleRDto;
  readonly quantity: number;
}

/**
 * Ligne valorisée : total de la ligne (règle du serveur), montant de remise (arrondi à l'ariary) et taux
 * (promotion en pourcentage seulement, 0 sinon).
 */
export interface PricedLine extends CartLine {
  readonly gross: number;
  readonly rate: number;
  readonly discount: number;
}

/** Code promo saisi et la promotion que le serveur lui associe. */
interface TypedPromoCode {
  readonly code: string;
  readonly promotion: PosPromotionRDto;
}

/** Message à annoncer aux lecteurs d'écran (clé + paramètres traduits à l'affichage). */
export interface CartAnnouncement {
  readonly key: string;
  readonly params: Readonly<Record<string, string | number>>;
}

/**
 * Ticket en cours d'un poste de caisse (état local, fourni par la page) : lignes, canal, paiement
 * et totaux recalculés à chaque changement (VTE-02, `pos-pricing`). Le serveur fait foi à l'encaissement.
 */
@Injectable()
export class PosCart {
  private readonly lines = signal<readonly CartLine[]>([]);
  private readonly promotions = signal<readonly PosPromotionRDto[]>([]);
  /** Code promo validé par le serveur (VTE-04) ; sa promotion concurrence l'automatique. */
  private readonly typedCode = signal<TypedPromoCode | null>(null);

  readonly channel = signal<SaleChannel>('SHOP');
  readonly payment = signal<PaymentMethod>('CASH');
  /** Vendeuse créditée de la vente (VTE-01) ; conservée d'un ticket à l'autre. */
  readonly seller = signal<string | null>(null);
  readonly received = signal(0);
  readonly announcement = signal<CartAnnouncement | null>(null);
  /** Vrai quand la saisie du code promo est ouverte (bouton ou F6). */
  readonly promoEntry = signal(false);

  private readonly grossLines = computed(() =>
    this.lines().map((line) => ({
      ...line,
      gross: lineTotal(line.article.price, line.quantity),
    })),
  );
  /** Promotion retenue par le poste (PRO-02, VTE-04) : la plus avantageuse, automatique ou par code, sans cumul. */
  private readonly applied = computed(() =>
    bestPromotion(
      this.promotions(),
      this.channel(),
      this.grossLines().map((line) => ({
        articleId: line.article.id,
        unitPrice: line.article.price,
        quantity: line.quantity,
        gross: line.gross,
      })),
      this.typedCode()?.promotion ?? null,
    ),
  );
  readonly priced = computed<readonly PricedLine[]>(() => {
    const { promotion, lineDiscounts } = this.applied();
    const rate = promotion?.kind === 'PERCENT' ? promotion.value : 0;
    return this.grossLines().map((line, index) => {
      const discount = lineDiscounts[index] ?? 0;
      return { ...line, rate: discount > 0 ? rate : 0, discount };
    });
  });
  readonly quantities = computed(
    () => new Map(this.lines().map((line) => [line.article.id, line.quantity])),
  );
  readonly isEmpty = computed(() => this.lines().length === 0);
  private readonly totals = computed(() =>
    ticketTotals(this.priced(), this.applied().ticketDiscount),
  );
  readonly subtotal = computed(() => this.totals().subtotal);
  readonly discount = computed(() => this.totals().discount);
  readonly total = computed(() => this.totals().total);
  readonly change = computed(() => changeDue(this.received(), this.total()));
  readonly appliedPromotion = computed(() =>
    this.discount() > 0 ? this.applied().promotion : null,
  );
  /** Code promo saisi, qu'il soit retenu ou non. */
  readonly promoCode = computed(() => this.typedCode()?.code ?? null);
  /** Code promo qui donne la remise du ticket ; `null` sans effet ou si l'automatique donne plus. */
  readonly appliedCode = computed(() => {
    const typed = this.typedCode();
    return typed && this.appliedPromotion()?.id === typed.promotion.id ? typed.code : null;
  });
  readonly canCheckout = computed(
    () =>
      !this.isEmpty() &&
      this.seller() !== null &&
      (this.payment() !== 'CASH' || this.change() >= 0),
  );

  /** Ouvre le poste : promotions automatiques du jour et éventuel ticket repris. */
  start(
    articles: readonly PosArticleRDto[],
    promotions: readonly PosPromotionRDto[],
    openTicket: readonly PosTicketLineRDto[],
    received: number,
  ): void {
    const byId = new Map(articles.map((article) => [article.id, article]));
    this.promotions.set(promotions);
    this.received.set(received);
    this.lines.set(
      openTicket.flatMap((line) => {
        const article = byId.get(line.articleId);
        return article ? [{ article, quantity: line.quantity }] : [];
      }),
    );
  }

  /** Retient le code promo validé par le serveur pour ce ticket ; ferme la saisie. */
  applyCode(code: string, promotion: PosPromotionRDto): void {
    this.typedCode.set({ code, promotion });
    this.promoEntry.set(false);
  }

  /** Retire le code promo du ticket. */
  removeCode(): void {
    this.typedCode.set(null);
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
    this.announce('pos.ticket.added', {
      name: article.name,
      quantity: quantity + 1,
      total: formatAriary(this.total()),
    });
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
      total: formatAriary(this.total()),
    });
  }

  /** Article de la dernière ligne (cible des raccourcis + / −). */
  lastArticle(): PosArticleRDto | undefined {
    return this.lines().at(-1)?.article;
  }

  clear(): void {
    this.lines.set([]);
    this.typedCode.set(null);
    this.promoEntry.set(false);
    this.received.set(0);
    this.payment.set('CASH');
  }

  private announce(key: string, params: Readonly<Record<string, string | number>>): void {
    this.announcement.set({ key, params });
  }
}
