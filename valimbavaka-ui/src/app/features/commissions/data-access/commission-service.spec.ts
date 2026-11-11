import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CommissionService } from './commission-service';

const COMMISSIONS_URL = '/api/v1/commissions';
const EMPTY_PAGE = { items: [], page: 1, pageSize: 20, totalCount: 0, totalPages: 0 };

describe('CommissionService', () => {
  let service: CommissionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CommissionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('envoie la pagination, la vendeuse et la période choisies', () => {
    service
      .list({ page: 2, size: 20, sellerId: 's1', from: '2026-11-01', to: '2026-11-30' })
      .subscribe();

    const request = httpMock.expectOne((req) => req.url === COMMISSIONS_URL);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('sellerId')).toBe('s1');
    expect(request.request.params.get('from')).toBe('2026-11-01');
    expect(request.request.params.get('to')).toBe('2026-11-30');
    request.flush(EMPTY_PAGE);
  });

  it("n'envoie ni vendeuse ni période quand elles ne sont pas choisies", () => {
    service.list({ page: 1, size: 20 }).subscribe();

    const request = httpMock.expectOne((req) => req.url === COMMISSIONS_URL);
    expect(request.request.params.has('sellerId')).toBe(false);
    expect(request.request.params.has('from')).toBe(false);
    expect(request.request.params.has('to')).toBe(false);
    request.flush(EMPTY_PAGE);
  });

  it('demande les vendeuses de la période avec leur statut', () => {
    service.sellers({ page: 1, size: 100 }, '2026-11-01', '2026-11-30').subscribe();

    const request = httpMock.expectOne((req) => req.url === `${COMMISSIONS_URL}/sellers`);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('size')).toBe('100');
    expect(request.request.params.get('from')).toBe('2026-11-01');
    expect(request.request.params.get('to')).toBe('2026-11-30');
    request.flush(EMPTY_PAGE);
  });

  it("envoie la validation de la vendeuse sur la période, sans rien d'autre", () => {
    const body = { sellerId: 's1', from: '2026-11-01', to: '2026-11-30' };

    service.validate(body).subscribe();

    const request = httpMock.expectOne(`${COMMISSIONS_URL}/validation`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush({ ...body, sellerName: 'Miora Rakoto', validatedCount: 2, validatedAmount: 850 });
  });

  it('demande le suivi du mois sans désigner de vendeuse (le serveur impose la sienne)', () => {
    service.followUp('2026-11').subscribe();

    const request = httpMock.expectOne((req) => req.url === `${COMMISSIONS_URL}/follow-up`);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('month')).toBe('2026-11');
    expect(request.request.params.has('sellerId')).toBe(false);
    request.flush({});
  });

  it("demande le suivi d'une vendeuse désignée par l'administrateur", () => {
    service.followUp('2026-10', 's1').subscribe();

    const request = httpMock.expectOne((req) => req.url === `${COMMISSIONS_URL}/follow-up`);
    expect(request.request.params.get('sellerId')).toBe('s1');
    expect(request.request.params.get('month')).toBe('2026-10');
    request.flush({});
  });

  it('demande le détail par vente du mois avec la pagination', () => {
    service.followUpLines({ page: 2, size: 20 }, '2026-11').subscribe();

    const request = httpMock.expectOne((req) => req.url === `${COMMISSIONS_URL}/follow-up/lines`);
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('20');
    expect(request.request.params.get('month')).toBe('2026-11');
    request.flush(EMPTY_PAGE);
  });
});
