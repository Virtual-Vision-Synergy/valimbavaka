import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthStore } from '../../../core/auth/auth-store';
import { buildUser } from '../../../core/auth/auth-testing';
import { InventorySessionWDto } from './inventory-models';
import { InventoryService } from './inventory-service';

const EMPTY_PAGE = { items: [], page: 1, pageSize: 20, totalCount: 0, totalPages: 0 };

describe('InventoryService', () => {
  let service: InventoryService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN')); // l'ouverture passe par la file hors ligne
    service = TestBed.inject(InventoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('envoie le statut et l’emplacement choisis dans la liste des sessions', () => {
    service.list({ page: 1, size: 20, status: 'IN_PROGRESS', locationId: 'loc-1' }).subscribe();

    const request = httpMock.expectOne((req) => req.url === '/api/v1/inventories');
    expect(request.request.params.get('status')).toBe('IN_PROGRESS');
    expect(request.request.params.get('locationId')).toBe('loc-1');
    request.flush(EMPTY_PAGE);
  });

  it('n’envoie ni statut ni emplacement quand ils sont absents', () => {
    service.list({ page: 1, size: 20 }).subscribe();

    const request = httpMock.expectOne((req) => req.url === '/api/v1/inventories');
    expect(request.request.params.has('status')).toBe(false);
    expect(request.request.params.has('locationId')).toBe(false);
    request.flush(EMPTY_PAGE);
  });

  it('lit les lignes paginées d’une session', () => {
    service.lines('session-1', { page: 2, size: 100 }).subscribe();

    const request = httpMock.expectOne((req) => req.url === '/api/v1/inventories/session-1/lines');
    expect(request.request.params.get('page')).toBe('2');
    request.flush(EMPTY_PAGE);
  });

  it('ouvre une session par POST avec une clé d’idempotence', async () => {
    const body: InventorySessionWDto = {
      locationId: 'loc-1',
      scope: 'ALL',
      rangeId: null,
      responsibleId: 'user-1',
      countDate: '2026-11-08',
    };

    const result = new Promise((resolve) => service.open(body).subscribe(resolve));

    const request = await vi.waitFor(() => httpMock.expectOne('/api/v1/inventories'));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    expect(request.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
    request.flush({ number: 'INV-0012' }, { status: 201, statusText: 'Created' });
    await expect(result).resolves.toMatchObject({ queued: false, body: { number: 'INV-0012' } });
  });
});
