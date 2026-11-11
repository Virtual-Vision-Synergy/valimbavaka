import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { InvoiceStatusTotalsRDto } from './invoice-status-models';
import { InvoiceStatusService } from './invoice-status-service';

const URL = '/api/v1/invoices/status-totals';
const TOTALS: InvoiceStatusTotalsRDto = {
  from: '2026-11-01',
  to: '2026-11-30',
  invoiceCount: 0,
  amount: 0,
  statuses: [],
};

describe('InvoiceStatusService', () => {
  let service: InvoiceStatusService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(InvoiceStatusService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lit les totaux par statut de la période demandée', () => {
    service.get({ from: '2026-11-01', to: '2026-11-30' }).subscribe();

    const request = httpMock.expectOne((req) => req.url === URL);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('from')).toBe('2026-11-01');
    expect(request.request.params.get('to')).toBe('2026-11-30');
    request.flush(TOTALS);
  });

  it("n'envoie pas de période vide : le serveur prend le mois en cours", () => {
    service.get({ from: '', to: undefined }).subscribe();

    const request = httpMock.expectOne((req) => req.url === URL);
    expect(request.request.params.has('from')).toBe(false);
    expect(request.request.params.has('to')).toBe(false);
    request.flush(TOTALS);
  });
});
