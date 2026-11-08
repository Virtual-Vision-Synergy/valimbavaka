import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthStore } from '../../../core/auth/auth-store';
import { buildUser } from '../../../core/auth/auth-testing';
import { ShootingRowRDto, ShootingSummaryRDto, ShootingWDto } from './service-models';
import { ShootingService } from './shooting-service';

/** SHO-01 : lecture des sorties et enregistrement par la file hors ligne (POST /api/v1/shootings). */
describe('ShootingService', () => {
  let service: ShootingService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    TestBed.inject(AuthStore).signIn('jeton', buildUser('SELLER')); // les écritures passent par la file
    service = TestBed.inject(ShootingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lit les 100 sorties les plus récentes et les convertit en lignes affichables', () => {
    let rows: readonly ShootingRowRDto[] = [];
    service.list().subscribe((value) => (rows = value));

    const request = httpMock.expectOne((req) => req.url === '/api/v1/shootings');
    expect(request.request.params.get('size')).toBe('100');
    expect(request.request.params.get('sortBy')).toBe('createdAt');
    expect(request.request.params.get('sortDirection')).toBe('DESC');
    request.flush({
      items: [
        {
          id: 'sho-1',
          number: 'SHO-0001',
          locationId: 'loc-store',
          items: [
            { articleId: 'a', articleCode: 'ART-0001', articleName: 'Savon noir', quantity: 2 },
          ],
          takenById: 'u',
          takenBy: 'Miora Andria',
          expectedReturnAt: '2026-11-08T13:00:00Z',
          note: null,
          status: 'OUT',
          late: true,
          createdAt: '2026-11-08T07:00:00Z',
          returnedAt: null,
          returnedBy: null,
        },
      ],
      page: 1,
      pageSize: 100,
      totalCount: 1,
      totalPages: 1,
    });

    expect(rows.map((row) => [row.number, row.status, row.items[0]?.name])).toEqual([
      ['SHO-0001', 'LATE', 'Savon noir'],
    ]);
  });

  it('lit les indicateurs des sorties en cours par GET /summary (SHO-03)', () => {
    let summary: ShootingSummaryRDto | undefined;
    service.summary().subscribe((value) => (summary = value));

    httpMock
      .expectOne((req) => req.method === 'GET' && req.url === '/api/v1/shootings/summary')
      .flush({ outCount: 3, lateCount: 1, immobilizedValue: 12241.5, unvaluedQuantity: 0 });

    expect(summary).toEqual({
      outCount: 3,
      lateCount: 1,
      immobilizedValue: 12241.5,
      unvaluedQuantity: 0,
    });
  });

  it('lit le stock par emplacement pour proposer les articles du formulaire', () => {
    service.stock().subscribe();

    const request = httpMock.expectOne((req) => req.url === '/api/v1/stocks');
    expect(request.request.params.get('sortBy')).toBe('name');
    request.flush({ items: [], page: 1, pageSize: 100, totalCount: 0, totalPages: 0 });
  });

  it('enregistre la sortie par POST, avec une clé d’idempotence', () => {
    const body: ShootingWDto = {
      locationId: 'loc-store',
      items: [{ articleId: 'a', quantity: 2 }],
      expectedReturnAt: '2026-11-08T13:00:00.000Z',
      note: null,
    };
    service.create(body).subscribe();

    const request = httpMock.expectOne('/api/v1/shootings');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    expect(request.request.headers.has('Idempotency-Key')).toBe(true);
    request.flush({});
  });

  it('réintègre une sortie par POST /{id}/return, sans corps et avec une clé d’idempotence (SHO-02)', () => {
    service.returnShooting('sho-1').subscribe();

    const request = httpMock.expectOne('/api/v1/shootings/sho-1/return');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    expect(request.request.headers.has('Idempotency-Key')).toBe(true);
    request.flush({});
  });
});
