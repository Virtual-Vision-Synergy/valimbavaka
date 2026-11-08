import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthStore } from '../../../core/auth/auth-store';
import { buildUser } from '../../../core/auth/auth-testing';
import { firstValueFrom } from 'rxjs';
import { InventoryLineRDto, InventorySessionRDto, InventorySessionWDto } from './inventory-models';
import { InventoryService } from './inventory-service';

const EMPTY_PAGE = { items: [], page: 1, pageSize: 20, totalCount: 0, totalPages: 0 };

const LINES_URL = '/api/v1/inventories/session-1/lines';

const SESSION: InventorySessionRDto = {
  id: 'session-1',
  number: 'INV-0012',
  locationId: 'loc-1',
  locationCode: 'STORE',
  locationLabel: 'Boutique',
  scope: 'ALL',
  rangeId: null,
  responsibleId: 'user-1',
  responsibleName: 'Hanta Rakoto',
  countDate: '2026-11-08',
  status: 'IN_PROGRESS',
  openedAt: '2026-11-08T06:30:00Z',
  openedBy: 'user-1',
  closedAt: null,
  lineCount: 2,
  gapCount: 0,
  version: 0,
};

function line(id: string, code: string): InventoryLineRDto {
  return {
    id,
    articleId: `article-${id}`,
    articleCode: code,
    articleName: `Savon ${code}`,
    barcode: null,
    unitCost: 3120.5,
    theoreticalQuantity: 12,
    countedQuantity: null,
    gap: null,
    reason: null,
  };
}

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

  it('enregistre le comptage d’une ligne par PUT avec une clé d’idempotence', async () => {
    const result = new Promise((resolve) =>
      service.count('session-1', 'line-1', { countedQuantity: 10 }).subscribe(resolve),
    );

    const request = await vi.waitFor(() =>
      httpMock.expectOne('/api/v1/inventories/session-1/lines/line-1/count'),
    );
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ countedQuantity: 10 });
    expect(request.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
    request.flush({ id: 'line-1', theoreticalQuantity: 12, countedQuantity: 10, gap: -2 });
    await expect(result).resolves.toMatchObject({ queued: false, body: { gap: -2 } });
  });

  it('enregistre le motif d’écart d’une ligne par PUT avec une clé d’idempotence', async () => {
    const result = new Promise((resolve) =>
      service.qualify('session-1', 'line-1', { reason: 'BREAKAGE' }).subscribe(resolve),
    );

    const request = await vi.waitFor(() =>
      httpMock.expectOne('/api/v1/inventories/session-1/lines/line-1/reason'),
    );
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ reason: 'BREAKAGE' });
    expect(request.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
    request.flush({ id: 'line-1', gap: -2, reason: 'BREAKAGE' });
    await expect(result).resolves.toMatchObject({ queued: false, body: { reason: 'BREAKAGE' } });
  });

  it('clôture une session par POST avec une clé d’idempotence et renvoie la session close', async () => {
    const result = new Promise((resolve) => service.close('session-1').subscribe(resolve));

    const request = await vi.waitFor(() =>
      httpMock.expectOne('/api/v1/inventories/session-1/close'),
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
    request.flush({ ...SESSION, status: 'CLOSED', closedAt: '2026-11-08T14:30:00Z' });
    await expect(result).resolves.toMatchObject({ queued: false, body: { status: 'CLOSED' } });
  });

  it('renvoie une feuille vide quand aucune session n’est en cours', async () => {
    const result = firstValueFrom(service.current());

    const request = httpMock.expectOne((req) => req.url === '/api/v1/inventories');
    expect(request.request.params.get('status')).toBe('IN_PROGRESS');
    request.flush(EMPTY_PAGE);
    await expect(result).resolves.toEqual({ session: null, previous: [] });
  });

  it('charge toutes les pages de lignes de la session en cours', async () => {
    const result = firstValueFrom(service.current());

    httpMock
      .expectOne((req) => req.url === '/api/v1/inventories')
      .flush({ ...EMPTY_PAGE, items: [SESSION], totalCount: 1, totalPages: 1 });
    httpMock
      .expectOne(
        (req) => req.url === '/api/v1/inventories' && req.params.get('status') === 'CLOSED',
      )
      .flush(EMPTY_PAGE);
    httpMock
      .expectOne((req) => req.url === LINES_URL && req.params.get('page') === '1')
      .flush({
        items: [line('l1', 'ART-0031')],
        page: 1,
        pageSize: 100,
        totalCount: 2,
        totalPages: 2,
      });
    httpMock
      .expectOne((req) => req.url === LINES_URL && req.params.get('page') === '2')
      .flush({
        items: [line('l2', 'ART-0087')],
        page: 2,
        pageSize: 100,
        totalCount: 2,
        totalPages: 2,
      });

    const context = await result;
    expect(context.session?.number).toBe('INV-0012');
    expect(context.session?.locationLabel).toBe('Boutique');
    expect(context.session?.openedBy).toBe('Hanta Rakoto');
    expect(context.session?.lines.map((item) => item.reference)).toEqual(['ART-0031', 'ART-0087']);
    expect(context.session?.lines[0]).toMatchObject({ lineId: 'l1', theoretical: 12, barcode: '' });
  });

  it('lit les dernières sessions clôturées avec leur nombre d’écarts à côté de la feuille', async () => {
    const closed: InventorySessionRDto = {
      ...SESSION,
      id: 'session-0',
      number: 'INV-0011',
      status: 'CLOSED',
      closedAt: '2026-11-01T14:30:00Z',
      gapCount: 3,
    };
    const result = firstValueFrom(service.current());

    httpMock
      .expectOne((req) => req.url === '/api/v1/inventories')
      .flush({ ...EMPTY_PAGE, items: [SESSION], totalCount: 1, totalPages: 1 });
    const previous = httpMock.expectOne(
      (req) => req.url === '/api/v1/inventories' && req.params.get('status') === 'CLOSED',
    );
    expect(previous.request.params.get('size')).toBe('5');
    previous.flush({ ...EMPTY_PAGE, items: [closed], totalCount: 1, totalPages: 1 });
    httpMock.expectOne((req) => req.url === LINES_URL).flush(EMPTY_PAGE);

    const context = await result;
    expect(context.previous.map((item) => [item.number, item.gapCount])).toEqual([['INV-0011', 3]]);
  });
});
