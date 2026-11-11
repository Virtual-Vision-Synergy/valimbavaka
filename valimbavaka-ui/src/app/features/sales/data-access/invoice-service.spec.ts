import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { InvoiceService } from './invoice-service';
import { InvoiceRDto } from './sales-models';

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

describe('InvoiceService', () => {
  let service: InvoiceService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(InvoiceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it("lit la facture d'une vente", () => {
    let received: InvoiceRDto | undefined;
    service.getBySale('vente-42').subscribe((invoice) => (received = invoice));

    const request = httpMock.expectOne('/api/v1/invoices/sales/vente-42');
    expect(request.request.method).toBe('GET');
    request.flush(INVOICE);

    expect(received?.number).toBe('FAC-2026-0042');
  });

  it('lit une facture par son identifiant', () => {
    service.getById('facture-1').subscribe();

    const request = httpMock.expectOne('/api/v1/invoices/facture-1');
    expect(request.request.method).toBe('GET');
    request.flush(INVOICE);
  });
});
