import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { CreditNoteRDto } from '../../data-access/credit-note-models';
import { CreditNoteDialog } from './credit-note-dialog';

const CREDIT_NOTE: CreditNoteRDto = {
  id: 'avoir-1',
  number: 'AV-2026-0001',
  invoiceId: 'facture-1',
  invoiceNumber: 'FAC-2026-0042',
  saleId: 'vente-42',
  saleNumber: 'VTE-0042',
  issuedAt: '2026-11-11T08:00:00Z',
  businessDate: '2026-11-11',
  reason: 'Produit défectueux',
  issuerName: 'By Valimbavaka Cosmetics',
  issuerNif: null,
  subtotal: 17000,
  discountAmount: 0,
  total: 17000,
  lines: [],
};

/** Dialogue d'avoir (FAC-04) : motif obligatoire, motif nettoyé envoyé, refus du serveur affiché sans rien émettre. */
describe('CreditNoteDialog', () => {
  let fixture: ComponentFixture<CreditNoteDialog>;
  let httpMock: HttpTestingController;
  let element: HTMLElement;
  const credited: CreditNoteRDto[] = [];

  const type = (value: string): void => {
    const area = element.querySelector<HTMLTextAreaElement>('textarea') as HTMLTextAreaElement;
    area.value = value;
    area.dispatchEvent(new Event('input'));
  };
  const submit = (): void => {
    (element.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    fixture.detectChanges();
  };

  beforeEach(async () => {
    credited.length = 0;
    await TestBed.configureTestingModule({
      imports: [CreditNoteDialog],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTestTranslations()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CreditNoteDialog);
    fixture.componentRef.setInput('invoiceId', 'facture-1');
    fixture.componentRef.setInput('invoiceNumber', 'FAC-2026-0042');
    fixture.componentInstance.credited.subscribe((note) => credited.push(note));
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it("refuse un motif d'espaces seuls : aucune requête, message d'erreur", () => {
    type('   ');
    submit();

    expect(element.querySelector('[role="alert"]')).not.toBeNull();
    expect(credited).toHaveLength(0);
  });

  it('envoie le motif nettoyé et annonce l’avoir émis', () => {
    type('  Produit défectueux   ');
    submit();

    const request = httpMock.expectOne('/api/v1/invoices/facture-1/credit-note');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ reason: 'Produit défectueux' });
    request.flush(CREDIT_NOTE);
    expect(credited).toEqual([CREDIT_NOTE]);
  });

  it('garde le dialogue ouvert avec le message quand le serveur refuse (409)', () => {
    type('Erreur de caisse');
    submit();

    httpMock
      .expectOne('/api/v1/invoices/facture-1/credit-note')
      .flush(
        { code: 'CONFLICT', detail: 'Déjà contre-passée' },
        { status: 409, statusText: 'Conflict' },
      );
    fixture.detectChanges();

    expect(credited).toHaveLength(0);
    expect(element.querySelector('.failure')).not.toBeNull();
  });
});
