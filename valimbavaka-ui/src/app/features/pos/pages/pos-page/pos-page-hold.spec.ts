import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NEVER, Observable, of } from 'rxjs';
import { vi } from 'vitest';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { SubmitResult } from '../../../../core/offline/outbox-models';
import { ToastService } from '../../../../shared/components/toast/toast-service';
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
  heldTickets: 1,
  articles: [SAVON],
  promotions: [],
  openTicket: [],
  openTicketReceived: 0,
};

/** Mise en attente d'un ticket (VTE-06) : le panier est vidé, aucune vente n'est enregistrée. */
describe('PosPage - mise en attente', () => {
  let fixture: ComponentFixture<PosPage>;
  let cart: PosCart;
  const record = vi.fn<() => Observable<SubmitResult<SaleRDto>>>();
  const show = vi.fn();
  const root = (): HTMLElement => fixture.nativeElement as HTMLElement;

  const settle = async (): Promise<void> => {
    fixture.detectChanges();
    await fixture.whenStable();
  };
  const pressF8 = async (): Promise<void> => {
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'F8', cancelable: true }));
    await settle();
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
    await settle();
    await settle();
    cart = fixture.debugElement.injector.get(PosCart);
  });

  it('F8 vide le panier sans enregistrer de vente et annonce le ticket mis en attente', async () => {
    cart.add(SAVON);

    await pressF8();

    expect(cart.isEmpty()).toBe(true);
    expect(record).not.toHaveBeenCalled();
    expect(show).toHaveBeenCalledWith('pos.toast.held', { ticket: 'VTE-42' }, 'info');
  });

  it('le bouton Mettre en attente donne le même résultat que F8 et passe au numéro suivant', async () => {
    cart.add(SAVON);
    await settle();

    root().querySelector<HTMLButtonElement>('button[aria-keyshortcuts="F8"]')?.click();
    await settle();

    expect(cart.isEmpty()).toBe(true);
    expect(record).not.toHaveBeenCalled();
    expect(root().textContent).toContain('VTE-43');
  });

  it('F8 sur un ticket vide ne change rien et prévient la caissière', async () => {
    await pressF8();

    expect(record).not.toHaveBeenCalled();
    expect(show).toHaveBeenCalledWith('pos.toast.holdEmpty', {}, 'warning');
    expect(show).not.toHaveBeenCalledWith('pos.toast.held', expect.anything(), 'info');
  });

  it('F8 pendant l’envoi d’un encaissement ne met pas le ticket en attente', async () => {
    record.mockReturnValue(NEVER);
    cart.add(SAVON);
    cart.payment.set('MVOLA');
    await settle();
    root().querySelector<HTMLButtonElement>('button.pay')?.click();
    await settle();

    await pressF8();

    expect(record).toHaveBeenCalledTimes(1);
    expect(show).not.toHaveBeenCalledWith('pos.toast.held', expect.anything(), 'info');
  });

  it('la vendeuse choisie est conservée pour le ticket suivant', async () => {
    cart.add(SAVON);

    await pressF8();

    expect(cart.seller()).toBe('seller-miora');
  });
});
