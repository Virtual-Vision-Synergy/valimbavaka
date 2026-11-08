import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthStore } from '../../../core/auth/auth-store';
import { buildUser } from '../../../core/auth/auth-testing';
import { SalonServiceWDto } from './salon-catalog-models';
import { SalonCatalogService } from './salon-catalog-service';

const SERVICES_URL = '/api/v1/salon-services';
const EMPTY_PAGE = { items: [], page: 1, pageSize: 10, totalCount: 0, totalPages: 0 };

describe('SalonCatalogService', () => {
  let service: SalonCatalogService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN')); // les écritures passent par la file hors ligne
    service = TestBed.inject(SalonCatalogService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('envoie la pagination, la recherche et le statut choisis', () => {
    service.list({ page: 2, size: 10, search: 'visage', active: true }).subscribe();

    const request = httpMock.expectOne((req) => req.url === SERVICES_URL);
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('search')).toBe('visage');
    expect(request.request.params.get('active')).toBe('true');
    request.flush(EMPTY_PAGE);
  });

  it("n'envoie pas de statut quand il n'est pas choisi", () => {
    service.list({ page: 1, size: 10 }).subscribe();

    const request = httpMock.expectOne((req) => req.url === SERVICES_URL);
    expect(request.request.params.has('active')).toBe(false);
    request.flush(EMPTY_PAGE);
  });

  it('crée un service par POST avec durée, prix, taux et produits consommés', () => {
    const body: SalonServiceWDto = {
      name: 'Soin visage éclat',
      durationMinutes: 45,
      price: 35000,
      commissionRate: 12.5,
      products: [{ articleId: 'a1', quantity: 15 }],
    };

    service.create(body).subscribe();

    const request = httpMock.expectOne(SERVICES_URL);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush({});
  });

  it('désactive un service via PATCH /status, sans suppression', () => {
    service.setActive('42', false).subscribe();

    const request = httpMock.expectOne(`${SERVICES_URL}/42/status`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ active: false });
    request.flush({});
  });
});
