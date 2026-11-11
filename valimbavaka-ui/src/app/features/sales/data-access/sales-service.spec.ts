import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SalesService } from './sales-service';

const SALES_URL = '/api/v1/sales';
const EMPTY_PAGE = { items: [], page: 1, pageSize: 10, totalCount: 0, totalPages: 0 };

describe('SalesService', () => {
  let service: SalesService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SalesService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it("lit une page des ventes d'un jour et d'un canal", () => {
    service
      .list({ page: 2, size: 10, date: '2026-11-08', salesChannelId: 'canal-facebook' })
      .subscribe();

    const request = httpMock.expectOne((req) => req.url === SALES_URL);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('10');
    expect(request.request.params.get('date')).toBe('2026-11-08');
    expect(request.request.params.get('salesChannelId')).toBe('canal-facebook');
    expect(request.request.params.has('sellerId')).toBe(false);
    request.flush(EMPTY_PAGE);
  });

  it("n'envoie ni jour ni canal vides : le serveur prend aujourd'hui et tous les canaux", () => {
    service.list({ page: 1, size: 10, date: '', salesChannelId: undefined }).subscribe();

    const request = httpMock.expectOne((req) => req.url === SALES_URL);
    expect(request.request.params.has('date')).toBe(false);
    expect(request.request.params.has('salesChannelId')).toBe(false);
    request.flush(EMPTY_PAGE);
  });

  it("lit le détail d'une vente par son identifiant", () => {
    service.detail('vente-42').subscribe();

    const request = httpMock.expectOne(`${SALES_URL}/vente-42/detail`);
    expect(request.request.method).toBe('GET');
    request.flush({ id: 'vente-42' });
  });
});
