import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { SubmitResult } from '../../../../core/offline/outbox-models';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { formatAriary } from '../../../../shared/pipes/ariary-pipe';
import { PosCart } from '../../data-access/pos-cart';
import {
  PosArticleRDto,
  PosContextRDto,
  SalePromotionRDto,
  SaleRDto,
  SaleWDto,
} from '../../data-access/pos-models';
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
  promotions: [],
  openTicket: [],
  openTicketReceived: 0,
};

const CODE_PROMOTION: SalePromotionRDto = {
  id: 'promo-code',
  name: 'Code Octobre',
  kind: 'PERCENT',
  value: 20,
  articleIds: [SAVON.id],
  channelIds: ['channel-store'],
};

const sale = (total: number): SaleRDto =>
  ({ number: 'VTE-0042', total, discountAmount: 8500 - total }) as SaleRDto;

/** Code promo au poste (VTE-04) : F6 ouvre la saisie, le serveur valide, le net baisse et le ticket l'indique. */
describe('PosPage - code promo', () => {
  let fixture: ComponentFixture<PosPage>;
  let cart: PosCart;
  const record = vi.fn<(body: SaleWDto) => Observable<SubmitResult<SaleRDto>>>();
  const checkPromoCode =
    vi.fn<(code: string, channelId: string) => Observable<SalePromotionRDto>>();
  const show = vi.fn();
  const root = (): HTMLElement => fixture.nativeElement as HTMLElement;

  const settle = async (): Promise<void> => {
    fixture.detectChanges();
    await fixture.whenStable();
  };
  const pressF6 = async (): Promise<void> => {
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'F6', cancelable: true }));
    await settle();
  };
  const typeCode = async (code: string): Promise<void> => {
    const field = root().querySelector<HTMLInputElement>('#pos-promo-code');
    if (field) {
      field.value = code;
    }
    root().querySelector<HTMLFormElement>('form.promo-entry')?.requestSubmit();
    await settle();
  };

  beforeEach(async () => {
    record.mockReset().mockReturnValue(of({ queued: false, body: sale(6800) }));
    checkPromoCode.mockReset().mockReturnValue(of(CODE_PROMOTION));
    show.mockReset();
    await TestBed.configureTestingModule({
      imports: [PosPage],
      providers: [
        provideRouter([]),
        provideTestTranslations(),
        { provide: PosService, useValue: { context: () => of(CONTEXT), record, checkPromoCode } },
        { provide: ToastService, useValue: { show, toasts: () => [] } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(PosPage);
    await settle();
    await settle();
    cart = fixture.debugElement.injector.get(PosCart);
    cart.add(SAVON);
    cart.payment.set('MVOLA');
    await settle();
  });

  it('F6 ouvre la saisie du code promo et lui donne le focus', async () => {
    await pressF6();

    expect(cart.promoEntry()).toBe(true);
    expect(document.activeElement).toBe(root().querySelector('#pos-promo-code'));
  });

  it('applique le code validé par le serveur : le net baisse et le ticket indique le code', async () => {
    await pressF6();

    await typeCode('octo20');

    expect(checkPromoCode).toHaveBeenCalledWith('OCTO20', 'channel-store');
    expect(cart.total()).toBe(6800);
    expect(root().querySelector('.promo')?.textContent).toContain('OCTO20');
    expect(root().querySelector('.net strong')?.textContent).toContain(formatAriary(6800).trim());
    expect(show).toHaveBeenCalledWith(
      'pos.toast.promoCodeApplied',
      { code: 'OCTO20', name: 'Code Octobre' },
      'info',
    );
  });

  it('refuse le code que le serveur rejette : le net ne change pas et la caissière est prévenue', async () => {
    checkPromoCode.mockReturnValue(throwError(() => new Error('Ce code promo est expiré.')));
    await pressF6();

    await typeCode('OLD2025');

    expect(cart.total()).toBe(8500);
    expect(cart.promoCode()).toBeNull();
    expect(show).toHaveBeenCalledWith(
      'pos.toast.promoCodeRefused',
      { message: 'Ce code promo est expiré.' },
      'warning',
    );
  });

  it('envoie le code retenu avec la vente, jamais un montant de remise', async () => {
    await pressF6();
    await typeCode('OCTO20');

    root().querySelector<HTMLButtonElement>('button.pay')?.click();
    await settle();

    expect(record).toHaveBeenCalledTimes(1);
    const body = record.mock.calls[0][0];
    expect(body.promoCode).toBe('OCTO20');
    expect(Object.keys(body).sort()).toEqual(
      ['lines', 'paymentMethod', 'promoCode', 'salesChannelId', 'sellerId'].sort(),
    );
    expect(cart.promoCode()).toBeNull();
  });

  it("n'envoie aucun code quand la vendeuse n'en a pas saisi", async () => {
    root().querySelector<HTMLButtonElement>('button.pay')?.click();
    await settle();

    expect(record.mock.calls[0][0].promoCode).toBeUndefined();
  });

  it('Échap ferme la saisie sans appeler le serveur', async () => {
    await pressF6();

    root()
      .querySelector('form.promo-entry')
      ?.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    await settle();

    expect(cart.promoEntry()).toBe(false);
    expect(checkPromoCode).not.toHaveBeenCalled();
  });
});
