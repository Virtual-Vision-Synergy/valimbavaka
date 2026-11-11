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
});
