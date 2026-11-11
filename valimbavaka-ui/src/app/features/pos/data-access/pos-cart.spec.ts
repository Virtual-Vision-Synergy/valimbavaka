import { TestBed } from '@angular/core/testing';
import { formatAriary } from '../../../shared/pipes/ariary-pipe';
import { PosCart } from './pos-cart';
import { POS_FIXTURE } from './pos-fixtures';
import { PosArticleRDto, PosPromotionRDto } from './pos-models';

const article = (id: string, name: string, price: number, stock = 10): PosArticleRDto => ({
  id,
  reference: id.toUpperCase(),
  barcode: '',
  name,
  price,
  range: 'BOTANIKA',
  stock,
  favorite: false,
  box: false,
});

const SAVON = article('art-0031', 'Savon noir 100 g', 8500, 2);
const GOMMAGE = article('art-0087', 'Gommage café 200 g', 12500);

/** Promotion de la maquette (Octobre rose, 15 %), ramenée aux articles de ce test. */
const promotion = (overrides: Partial<PosPromotionRDto> = {}): PosPromotionRDto => ({
  ...POS_FIXTURE.promotions[0],
  articleIds: [SAVON.id, GOMMAGE.id],
  ...overrides,
});

/** Calcul automatique du ticket en caisse (VTE-02) : chaque changement recalcule les totaux. */
describe('PosCart', () => {
  let cart: PosCart;

  const totals = () => ({
    subtotal: cart.subtotal(),
    discount: cart.discount(),
    total: cart.total(),
  });

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [PosCart] });
    cart = TestBed.inject(PosCart);
    cart.start([SAVON, GOMMAGE], [], [], 0);
  });

  it("met à jour sous-total, remise et net à payer à chaque ajout d'article", () => {
    cart.add(SAVON);
    expect(totals()).toEqual({ subtotal: 8500, discount: 0, total: 8500 });

    cart.add(SAVON);
    expect(totals()).toEqual({ subtotal: 17000, discount: 0, total: 17000 });

    cart.add(GOMMAGE);
    expect(totals()).toEqual({ subtotal: 29500, discount: 0, total: 29500 });
  });

  it('applique la remise de ligne de la promotion aux seuls articles ciblés', () => {
    cart.start(
      POS_FIXTURE.articles,
      POS_FIXTURE.promotions,
      POS_FIXTURE.openTicket,
      POS_FIXTURE.openTicketReceived,
    );

    expect(totals()).toEqual({ subtotal: 64000, discount: 5700, total: 58300 });
    const discounted = cart.priced().filter((line) => line.discount > 0);
    expect(discounted.map((line) => line.discount)).toEqual([3825, 1875]);
    expect(cart.appliedPromotion()?.name).toBe('Octobre rose');
  });

  it("applique la promotion seulement sur les canaux qu'elle vise", () => {
    const facebookOnly = promotion({ channels: ['FACEBOOK'] });
    cart.start([SAVON, GOMMAGE], [facebookOnly], [], 0);
    cart.add(SAVON);
    expect(cart.discount()).toBe(0);
    expect(cart.appliedPromotion()).toBeNull();

    cart.channel.set('FACEBOOK');

    expect(cart.discount()).toBe(1275);
    expect(cart.total()).toBe(7225);
    expect(cart.appliedPromotion()?.name).toBe('Octobre rose');
  });

  it("retire un montant fixe une seule fois du ticket et ne l'affiche sur aucune ligne", () => {
    const amount = promotion({ kind: 'AMOUNT', value: 2000 });
    cart.start([SAVON, GOMMAGE], [amount], [], 0);

    cart.add(SAVON);
    cart.add(GOMMAGE);

    expect(totals()).toEqual({ subtotal: 21000, discount: 2000, total: 19000 });
    expect(cart.priced().map((line) => line.discount)).toEqual([0, 0]);
  });

  it('offre une unité par groupe de deux achetées avec une promotion BUY_X', () => {
    const savon = { ...SAVON, stock: 10 };
    const buyTwo = promotion({ kind: 'BUY_X', value: 2, articleIds: [savon.id] });
    cart.start([savon], [buyTwo], [], 0);

    cart.add(savon);
    expect(cart.discount()).toBe(0);
    cart.add(savon);

    expect(cart.priced()[0]).toMatchObject({ discount: 8500, rate: 0 });
    expect(cart.total()).toBe(8500);
  });

  it("choisit la promotion la plus avantageuse quand plusieurs s'appliquent", () => {
    const percent = promotion();
    const amount = promotion({ id: 'montant', name: 'Mille ariary', kind: 'AMOUNT', value: 1000 });
    cart.start([SAVON, GOMMAGE], [amount, percent], [], 0);

    cart.add(SAVON);

    expect(cart.appliedPromotion()?.name).toBe('Octobre rose');
    expect(cart.discount()).toBe(1275);
  });

  it("recalcule les totaux quand une unité est retirée, jusqu'au ticket vide", () => {
    cart.add(SAVON);
    cart.add(SAVON);
    cart.add(GOMMAGE);
    expect(cart.total()).toBe(29500);

    cart.decrement(SAVON.id);
    expect(cart.total()).toBe(21000);
    cart.decrement(SAVON.id);
    expect(cart.total()).toBe(12500);
    cart.decrement(GOMMAGE.id);

    expect(totals()).toEqual({ subtotal: 0, discount: 0, total: 0 });
    expect(cart.isEmpty()).toBe(true);
  });

  it('refuse un ajout au-delà du stock sans changer les totaux', () => {
    cart.add(SAVON);
    cart.add(SAVON);

    const added = cart.add(SAVON);

    expect(added).toBe(false);
    expect(cart.total()).toBe(17000);
    expect(cart.announcement()).toEqual({
      key: 'pos.toast.outOfStock',
      params: { name: 'Savon noir 100 g' },
    });
  });

  it("calcule le rendu monnaie et bloque l'encaissement en espèces si le reçu ne couvre pas le net", () => {
    cart.start(POS_FIXTURE.articles, POS_FIXTURE.promotions, POS_FIXTURE.openTicket, 60000);
    cart.seller.set('seller-miora');
    expect(cart.change()).toBe(1700);
    expect(cart.canCheckout()).toBe(true);

    cart.received.set(50000);
    expect(cart.change()).toBe(-8300);
    expect(cart.canCheckout()).toBe(false);

    cart.payment.set('MVOLA');
    expect(cart.canCheckout()).toBe(true);
  });

  it("annonce le net à payer aux lecteurs d'écran après chaque ajout ou retrait", () => {
    cart.add(SAVON);
    expect(cart.announcement()).toEqual({
      key: 'pos.ticket.added',
      params: { name: 'Savon noir 100 g', quantity: 1, total: formatAriary(8500) },
    });

    cart.add(GOMMAGE);
    cart.decrement(SAVON.id);
    expect(cart.announcement()?.key).toBe('pos.ticket.removed');
    expect(cart.announcement()?.params['total']).toBe(formatAriary(12500));
  });

  it('remet les totaux à zéro au ticket suivant en gardant la vendeuse', () => {
    cart.seller.set('seller-miora');
    cart.add(GOMMAGE);
    cart.received.set(20000);
    cart.payment.set('CARD');

    cart.clear();

    expect(totals()).toEqual({ subtotal: 0, discount: 0, total: 0 });
    expect(cart.received()).toBe(0);
    expect(cart.change()).toBe(0);
    expect(cart.payment()).toBe('CASH');
    expect(cart.seller()).toBe('seller-miora');
    expect(cart.canCheckout()).toBe(false);
  });
});
