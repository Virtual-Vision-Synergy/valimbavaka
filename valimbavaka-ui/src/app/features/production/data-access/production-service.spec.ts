import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthStore } from '../../../core/auth/auth-store';
import { buildUser } from '../../../core/auth/auth-testing';
import { CreatedLot, ProductionService } from './production-service';
import { ProductionContextRDto } from './production-models';

const page = (items: unknown[]) => ({
  items,
  page: 1,
  pageSize: 100,
  totalCount: items.length,
  totalPages: 1,
});

describe('ProductionService', () => {
  let service: ProductionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    TestBed.inject(AuthStore).signIn('jeton', buildUser('SELLER')); // la création passe par la file hors ligne
    service = TestBed.inject(ProductionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it("assemble l'écran : produits finis, entrants avec le stock de l'entrepôt, vendeuses, lots récents", () => {
    let context: ProductionContextRDto | undefined;
    service.context().subscribe((value) => (context = value));

    const articles = httpMock.match((req) => req.url === '/api/v1/articles/production');
    expect(articles.map((req) => req.request.params.get('usage'))).toEqual(['OUTPUT', 'INPUT']);
    articles[0].flush(
      page([
        {
          id: 'pf-1',
          code: 'ART-0031',
          name: 'Savon noir',
          unit: 'PIECE',
          price: 8500,
          cost: null,
        },
      ]),
    );
    articles[1].flush(
      page([
        { id: 'mp-1', code: 'ART-0008', name: 'Base savonneuse', unit: 'KILOGRAM', cost: 12000 },
      ]),
    );
    httpMock
      .expectOne((req) => req.url === '/api/v1/stocks')
      .flush(
        page([
          {
            articleId: 'mp-1',
            locations: [{ locationId: 'l1', code: 'WAREHOUSE', label: 'Entrepôt', quantity: 12.5 }],
          },
        ]),
      );
    httpMock
      .expectOne((req) => req.url === '/api/v1/production/operators')
      .flush(page([{ id: 'op-1', firstName: 'Soa', lastName: 'Rakoto' }]));
    const batches = httpMock.expectOne((req) => req.url === '/api/v1/production/batches');
    expect(batches.request.params.get('size')).toBe('10');
    batches.flush(
      page([
        {
          id: 'b1',
          number: 'LOT-00003',
          outputArticleId: 'pf-1',
          quantity: 24,
          operatorId: 'op-1',
          status: 'PLANNED',
          inputs: [],
          createdAt: '2026-11-07T10:00:00Z',
        },
      ]),
    );

    expect(context?.products[0].articleId).toBe('pf-1');
    expect(context?.materials[0]).toMatchObject({ id: 'mp-1', unit: 'kg', available: 12.5 });
    expect(context?.operators).toEqual([{ id: 'op-1', name: 'Soa Rakoto' }]);
    expect(context?.recentLots[0]).toMatchObject({
      number: 'LOT-00003',
      articleName: 'Savon noir',
      operator: 'Soa Rakoto',
      status: 'DRAFT',
    });
    expect(context?.kpis.lotsThisMonth).toBe(0);
  });

  it('crée le lot par POST avec une clé d’idempotence et renvoie le lot planifié du serveur', () => {
    let created: CreatedLot | undefined;
    service
      .createLot(
        {
          articleId: 'pf-1',
          quantity: 30,
          operatorId: 'op-1',
          lines: [{ materialId: 'mp-1', quantity: 2.4 }],
          validate: false,
        },
        { articleName: 'Savon noir', operator: 'Soa Rakoto' },
      )
      .subscribe((value) => (created = value));

    const request = httpMock.expectOne('/api/v1/production/batches');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.has('Idempotency-Key')).toBe(true);
    expect(request.request.body).toEqual({
      outputArticleId: 'pf-1',
      quantity: 30,
      operatorId: 'op-1',
      inputs: [{ articleId: 'mp-1', quantity: 2.4 }],
    });
    request.flush({
      id: 'b2',
      number: 'LOT-00004',
      outputArticleId: 'pf-1',
      quantity: 30,
      operatorId: 'op-1',
      status: 'PLANNED',
      inputs: [{ articleId: 'mp-1', quantity: 2.4 }],
      createdAt: '2026-11-08T08:00:00Z',
    });

    expect(created?.queued).toBe(false);
    expect(created?.lot).toMatchObject({
      number: 'LOT-00004',
      articleName: 'Savon noir',
      operator: 'Soa Rakoto',
      status: 'DRAFT',
    });
  });

  it('« Valider le lot » crée et valide en une seule requête POST sur /completed-batches', () => {
    let created: CreatedLot | undefined;
    service
      .createLot(
        {
          articleId: 'pf-1',
          quantity: 12,
          operatorId: 'op-1',
          lines: [{ materialId: 'mp-1', quantity: 3 }],
          validate: true,
        },
        { articleName: 'Savon noir', operator: 'Soa Rakoto' },
      )
      .subscribe((value) => (created = value));

    const request = httpMock.expectOne('/api/v1/production/completed-batches');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.has('Idempotency-Key')).toBe(true);
    expect(request.request.body).toEqual({
      outputArticleId: 'pf-1',
      quantity: 12,
      operatorId: 'op-1',
      inputs: [{ articleId: 'mp-1', quantity: 3 }],
    });
    request.flush({
      id: 'b5',
      number: 'LOT-00005',
      outputArticleId: 'pf-1',
      quantity: 12,
      operatorId: 'op-1',
      status: 'COMPLETED',
      inputs: [{ articleId: 'mp-1', quantity: 3, unitCost: 12000 }],
      completedAt: '2026-11-08T08:00:00Z',
      createdAt: '2026-11-08T08:00:00Z',
    });

    expect(created?.lot).toMatchObject({ number: 'LOT-00005', status: 'VALIDATED' });
  });

  it('démarre un lot planifié par POST sur /batches/{id}/start avec une clé d’idempotence', () => {
    let status: string | undefined;
    service.startLot('b2').subscribe((result) => (status = result.body?.status));

    const request = httpMock.expectOne('/api/v1/production/batches/b2/start');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.has('Idempotency-Key')).toBe(true);
    request.flush({ id: 'b2', status: 'IN_PROGRESS' });

    expect(status).toBe('IN_PROGRESS');
  });

  it('valide un lot planifié existant par POST sur /batches/{id}/completion', () => {
    let status: string | undefined;
    service.completeLot('b2').subscribe((result) => (status = result.body?.status));

    const request = httpMock.expectOne('/api/v1/production/batches/b2/completion');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.has('Idempotency-Key')).toBe(true);
    request.flush({ id: 'b2', status: 'COMPLETED' });

    expect(status).toBe('COMPLETED');
  });
});
