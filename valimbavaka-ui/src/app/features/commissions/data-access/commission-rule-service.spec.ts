import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthStore } from '../../../core/auth/auth-store';
import { buildUser } from '../../../core/auth/auth-testing';
import { CommissionRuleService } from './commission-rule-service';

const RULES_URL = '/api/v1/commission-rules';
const EMPTY_PAGE = { items: [], page: 1, pageSize: 10, totalCount: 0, totalPages: 0 };

describe('CommissionRuleService', () => {
  let service: CommissionRuleService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN')); // les écritures passent par la file hors ligne
    service = TestBed.inject(CommissionRuleService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('envoie la pagination, la recherche, le canal et le statut choisis', () => {
    service
      .list({ page: 2, size: 10, search: 'Boutique', salesChannelId: 'c1', active: false })
      .subscribe();

    const request = httpMock.expectOne((req) => req.url === RULES_URL);
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('search')).toBe('Boutique');
    expect(request.request.params.get('salesChannelId')).toBe('c1');
    expect(request.request.params.get('active')).toBe('false');
    request.flush(EMPTY_PAGE);
  });

  it("n'envoie ni canal ni statut quand ils ne sont pas choisis", () => {
    service.list({ page: 1, size: 10 }).subscribe();

    const request = httpMock.expectOne((req) => req.url === RULES_URL);
    expect(request.request.params.has('salesChannelId')).toBe(false);
    expect(request.request.params.has('active')).toBe(false);
    request.flush(EMPTY_PAGE);
  });

  it('crée une règle par POST avec nom, canal, taux et base de calcul', () => {
    const body = {
      name: 'Boutique standard',
      salesChannelId: 'c1',
      rate: 3.5,
      base: 'REVENUE',
    } as const;

    service.create(body).subscribe();

    const request = httpMock.expectOne(RULES_URL);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush({});
  });

  it('désactive une règle via PATCH /status, sans suppression', () => {
    service.setActive('42', false).subscribe();

    const request = httpMock.expectOne(`${RULES_URL}/42/status`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ active: false });
    request.flush({});
  });
});
