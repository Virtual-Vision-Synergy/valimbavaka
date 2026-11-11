import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of } from 'rxjs';
import { vi } from 'vitest';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { SubmitResult } from '../../../../core/offline/outbox-models';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { formatAriary } from '../../../../shared/pipes/ariary-pipe';
import { PosCart } from '../../data-access/pos-cart';
import { PosArticleRDto, PosContextRDto, SaleRDto } from '../../data-access/pos-models';
import { PosService } from '../../data-access/pos-service';
import { PosPage } from './pos-page';

const SAVON: PosArticleRDto = {
  id: 'art-0031',
  reference: 'ART-0031',
  barcode: '',
  name: 'Savon noir 100 g',
  price: 8500,
  range: 'BOTANIKA',
  stock: 10,
  favorite: false,
  box: false,
};

const CONTEXT: PosContextRDto = {
  location: 'Boutique',
  cashierName: 'Miora Rakoto',
  sellers: [{ id: 'seller-miora', name: 'Miora Rakoto' }],
  defaultSellerId: 'seller-miora',
  channelIds: { SHOP: 'channel-store' },
  ticketNumber: 42,
  openedAt: '08:30',
  heldTickets: 0,
  articles: [SAVON],
  promotion: null,
  openTicket: [],
  openTicketReceived: 0,
};

const sale = (total: number): SaleRDto => ({
  id: 'sale-42',
  number: 'VTE-0042',
  businessDayId: 'day-1',
  businessDate: '2026-11-08',
  salesChannelId: 'channel-store',
  stockLocationId: 'location-store',
  sellerId: 'seller-miora',
  paymentMethod: 'MVOLA',
  status: 'VALIDATED',
  subtotal: total,
  discountAmount: 0,
  total,
  lines: [{ articleId: SAVON.id, quantity: 1, unitPrice: total, lineTotal: total }],
  createdAt: '2026-11-08T08:35:00Z',
  createdBy: 'seller-miora',
});

/** Encaissement (VTE-02) : le net du serveur fait foi, un écart avec le net affiché est signalé. */
describe('PosPage', () => {
  let fixture: ComponentFixture<PosPage>;
  let cart: PosCart;
  const record = vi.fn<() => Observable<SubmitResult<SaleRDto>>>();
  const show = vi.fn();

  const checkoutOneSoap = async (): Promise<void> => {
    cart.add(SAVON);
    cart.payment.set('MVOLA');
    fixture.detectChanges();
    await fixture.whenStable();
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.pay')?.click();
    fixture.detectChanges();
    await fixture.whenStable();
  };

  beforeEach(async () => {
    record.mockReset();
    show.mockReset();
    await TestBed.configureTestingModule({
      imports: [PosPage],
      providers: [
        provideRouter([]),
        provideTestTranslations(),
        { provide: PosService, useValue: { context: () => of(CONTEXT), record } },
        { provide: ToastService, useValue: { show, toasts: () => [] } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(PosPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    cart = fixture.debugElement.injector.get(PosCart);
  });

  it('avertit quand le net encaissé par le serveur diffère du net affiché', async () => {
    record.mockReturnValue(of({ queued: false, body: sale(9000) }));

    await checkoutOneSoap();

    expect(record).toHaveBeenCalledTimes(1);
    expect(show).toHaveBeenCalledWith(
      'pos.toast.totalAdjusted',
      { ticket: 'VTE-0042', amount: formatAriary(9000), displayed: formatAriary(8500) },
      'warning',
    );
  });

  it("confirme l'encaissement et remet les totaux à zéro quand le serveur confirme le net affiché", async () => {
    record.mockReturnValue(of({ queued: false, body: sale(8500) }));

    await checkoutOneSoap();

    expect(show).toHaveBeenCalledWith('pos.toast.paid', {
      ticket: 'VTE-0042',
      amount: formatAriary(8500),
    });
    expect(cart.total()).toBe(0);
    expect(cart.isEmpty()).toBe(true);
  });

  it.each(['MVOLA', 'ORANGE_MONEY', 'CARD'] as const)(
    'enregistre la vente avec le mode de règlement choisi (%s)',
    async (method) => {
      record.mockReturnValue(of({ queued: false, body: sale(8500) }));
      cart.add(SAVON);
      cart.payment.set(method);
      fixture.detectChanges();
      await fixture.whenStable();

      (fixture.nativeElement as HTMLElement)
        .querySelector<HTMLButtonElement>('button.pay')
        ?.click();
      fixture.detectChanges();
      await fixture.whenStable();

      expect(record).toHaveBeenCalledWith(expect.objectContaining({ paymentMethod: method }));
    },
  );

  it('enregistre la vente en espèces par défaut quand le reçu couvre le net', async () => {
    record.mockReturnValue(of({ queued: false, body: sale(8500) }));
    cart.add(SAVON);
    cart.received.set(10000);
    fixture.detectChanges();
    await fixture.whenStable();

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.pay')?.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(record).toHaveBeenCalledWith(expect.objectContaining({ paymentMethod: 'CASH' }));
  });

  it("ne compare pas les montants d'une vente mise en file hors connexion", async () => {
    record.mockReturnValue(of({ queued: true }));

    await checkoutOneSoap();

    expect(show).toHaveBeenCalledWith('common.queuedSaved', {}, 'info');
    expect(show).not.toHaveBeenCalledWith('pos.toast.totalAdjusted', expect.anything(), 'warning');
  });

  describe('raccourcis clavier (VTE-03)', () => {
    const root = (): HTMLElement => fixture.nativeElement as HTMLElement;
    const press = async (key: string, init: KeyboardEventInit = {}): Promise<KeyboardEvent> => {
      const event = new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true, ...init });
      document.dispatchEvent(event);
      fixture.detectChanges();
      await fixture.whenStable();
      return event;
    };

    it('F2 donne le focus au champ de recherche', async () => {
      const event = await press('F2');

      expect(event.defaultPrevented).toBe(true);
      expect(document.activeElement).toBe(root().querySelector('input'));
    });

    it('+ ajoute une unité du dernier article et − la retire', async () => {
      cart.add(SAVON);

      await press('+');
      expect(cart.total()).toBe(17000);

      await press('-');
      await press('-');
      expect(cart.isEmpty()).toBe(true);
    });

    it('+ sur un ticket vide ouvre la recherche pour ajouter un article', async () => {
      await press('+');

      expect(cart.isEmpty()).toBe(true);
      expect(document.activeElement).toBe(root().querySelector('input'));
    });

    it('F12 encaisse la vente comme le bouton', async () => {
      record.mockReturnValue(of({ queued: false, body: sale(8500) }));
      cart.add(SAVON);
      cart.payment.set('MVOLA');

      await press('F12');

      expect(record).toHaveBeenCalledTimes(1);
      expect(cart.isEmpty()).toBe(true);
    });

    it('F12 maintenu enfoncé n’encaisse qu’une fois', async () => {
      record.mockReturnValue(of({ queued: false, body: sale(8500) }));
      cart.add(SAVON);
      cart.payment.set('MVOLA');

      await press('F12');
      await press('F12', { repeat: true });

      expect(record).toHaveBeenCalledTimes(1);
    });

    it('F12 sur un ticket vide n’enregistre rien et prévient la caissière', async () => {
      await press('F12');

      expect(record).not.toHaveBeenCalled();
      expect(show).toHaveBeenCalledWith('pos.toast.cannotCheckout', {}, 'warning');
    });

    it('Ctrl − reste le zoom du navigateur : le ticket ne change pas', async () => {
      cart.add(SAVON);

      const event = await press('-', { ctrlKey: true });

      expect(event.defaultPrevented).toBe(false);
      expect(cart.total()).toBe(8500);
    });
  });
});
