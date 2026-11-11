import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { InvoiceRDto, SaleSummaryRDto } from '../../data-access/sales-models';
import { InvoicePanel } from './invoice-panel';

const SALE: SaleSummaryRDto = {
  id: 'vente-42',
  number: 'VTE-0042',
  businessDate: '2026-11-08',
  createdAt: '2026-11-08T07:52:00Z',
  salesChannelId: 'canal-boutique',
  salesChannelLabel: 'Boutique',
  sellerId: 'miora',
  sellerName: 'Miora Rakoto',
  paymentMethod: 'MVOLA',
  status: 'VALIDATED',
  subtotal: 17000,
  discountAmount: 0,
  total: 17000,
  lines: [{ articleId: 'savon', articleName: 'Savon coco', quantity: 2, lineTotal: 17000 }],
};

const INVOICE: InvoiceRDto = {
  id: 'facture-1',
  number: 'FAC-2026-0042',
  saleId: 'vente-42',
  saleNumber: 'VTE-0042',
  issuedAt: '2026-11-08T07:52:00Z',
  businessDate: '2026-11-08',
  status: 'VALIDATED',
  salesChannelLabel: 'Boutique',
  sellerId: 'miora',
  sellerName: 'Miora Rakoto',
  paymentMethod: 'MVOLA',
  issuerName: 'By Valimbavaka Cosmetics',
  issuerNif: null,
  subtotal: 17000,
  discountAmount: 0,
  total: 17000,
  lines: [
    { articleId: 'savon', label: 'Savon coco', quantity: 2, unitPrice: 8500, lineTotal: 17000 },
  ],
};

/** Panneau facture (FAC-01) : la facture émise par le serveur, ou l'aperçu d'une vente qui n'en a pas. */
describe('InvoicePanel', () => {
  let fixture: ComponentFixture<InvoicePanel>;
  let httpMock: HttpTestingController;
  let element: HTMLElement;

  const text = (selector: string): string =>
    element
      .querySelector(selector)
      ?.textContent?.replace(/[\t\n ]+/g, ' ')
      .trim() ?? '';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InvoicePanel],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTestTranslations()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(InvoicePanel);
    fixture.componentRef.setInput('sale', SALE);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it("affiche le numéro de la facture émise et les lignes figées à l'émission", async () => {
    httpMock.expectOne('/api/v1/invoices/sales/vente-42').flush(INVOICE);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(text('#invoice-title .mono')).toBe('FAC-2026-0042');
    expect(text('.paper .ident .mono')).toBe('FAC-2026-0042');
    expect(text('.paper .lines tbody')).toContain('Savon coco');
    expect(element.querySelector('.note')?.textContent).not.toContain('Aucune facture');
  });

  it("montre l'aperçu « À émettre » et son explication quand la vente n'a pas de facture", async () => {
    httpMock
      .expectOne('/api/v1/invoices/sales/vente-42')
      .flush({ code: 'NOT_FOUND' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(text('#invoice-title .mono')).toBe('VTE-0042');
    expect(text('.paper .ident .mono')).toBe('À émettre');
    expect(text('.note')).toContain("Aucune facture n'a été émise");
  });
});
