import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { formatAriary } from '../../../../shared/pipes/ariary-pipe';
import { PosCart } from '../../data-access/pos-cart';
import { PosArticleRDto } from '../../data-access/pos-models';
import { PosTicket } from './pos-ticket';

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

/** Ticket du poste de caisse (VTE-02) : les montants affichés suivent le panier sans action de la vendeuse. */
describe('PosTicket', () => {
  let fixture: ComponentFixture<PosTicket>;
  let cart: PosCart;
  let element: HTMLElement;

  const text = (selector: string): string =>
    element
      .querySelector(selector)
      ?.textContent?.replace(/[\t\n ]+/g, ' ')
      .trim() ?? '';

  const totalsShown = () => {
    const values = Array.from(element.querySelectorAll('.totals dd.num')).map((dd) =>
      dd.textContent?.trim(),
    );
    return { subtotal: values[0], discount: values[1], total: text('.net strong') };
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PosTicket],
      providers: [PosCart, provideTestTranslations()],
    }).compileComponents();
    cart = TestBed.inject(PosCart);
    cart.start([SAVON], null, [], 0);
    fixture = TestBed.createComponent(PosTicket);
    fixture.componentRef.setInput('ticketNumber', 42);
    fixture.componentRef.setInput('cashierName', 'Miora Rakoto');
    fixture.componentRef.setInput('openedAt', '08:30');
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it("affiche sous-total, remise et net à payer mis à jour dès l'ajout d'un article", async () => {
    expect(totalsShown()).toEqual({
      subtotal: formatAriary(0),
      discount: `−${formatAriary(0)}`,
      total: formatAriary(0),
    });

    cart.add(SAVON);
    cart.add(SAVON);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(totalsShown()).toEqual({
      subtotal: formatAriary(17000),
      discount: `−${formatAriary(0)}`,
      total: formatAriary(17000),
    });
    expect(text('.line-total')).toBe(formatAriary(17000));
    expect(text('button.pay')).toContain(`Encaisser ${formatAriary(17000)}`);
  });

  it('affiche le rendu ou le manque selon le montant reçu', async () => {
    cart.add(SAVON);
    cart.received.set(10000);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(text('output.change')).toBe(formatAriary(1500));

    cart.received.set(5000);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(text('output.change')).toBe(`Manque ${formatAriary(3500)}`);
    expect(element.querySelector('output.change')?.classList).toContain('missing');
  });

  describe('mode de règlement (VTE-05)', () => {
    const radios = (): HTMLInputElement[] =>
      Array.from(element.querySelectorAll<HTMLInputElement>('input[name="pos-payment"]'));

    const choose = async (method: string): Promise<void> => {
      radios()
        .find((radio) => radio.value === method)
        ?.click();
      fixture.detectChanges();
      await fixture.whenStable();
    };

    it("propose espèces, MVola, Orange Money et carte, espèces étant choisi à l'ouverture", () => {
      expect(radios().map((radio) => radio.value)).toEqual([
        'CASH',
        'MVOLA',
        'ORANGE_MONEY',
        'CARD',
      ]);
      expect(
        radios()
          .filter((radio) => radio.checked)
          .map((radio) => radio.value),
      ).toEqual(['CASH']);
      expect(text('.payment.selected')).toBe('Espèces');
    });

    it('enregistre le mode choisi dans le ticket, un seul mode à la fois', async () => {
      await choose('CARD');

      expect(cart.payment()).toBe('CARD');
      expect(
        radios()
          .filter((radio) => radio.checked)
          .map((radio) => radio.value),
      ).toEqual(['CARD']);
      expect(text('.payment.selected')).toBe('Carte');
    });

    it('masque la saisie du reçu hors espèces et encaisse sans montant reçu', async () => {
      cart.add(SAVON);
      fixture.detectChanges();
      await fixture.whenStable();
      expect(element.querySelector('#pos-received')).not.toBeNull();
      expect(element.querySelector<HTMLButtonElement>('button.pay')?.disabled).toBe(true);

      await choose('MVOLA');

      expect(element.querySelector('#pos-received')).toBeNull();
      expect(element.querySelector<HTMLButtonElement>('button.pay')?.disabled).toBe(false);
    });

    it('ne propose pas « à la livraison », réservé au livreur', () => {
      expect(radios().map((radio) => radio.value)).not.toContain('ON_DELIVERY');
    });
  });
});
