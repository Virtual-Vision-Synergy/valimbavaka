import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ArticleWDto } from './article-models';
import { ArticleService } from './article-service';

const ARTICLES_URL = '/api/v1/articles';
const EMPTY_PAGE = { items: [], page: 1, pageSize: 20, totalCount: 0, totalPages: 0 };

const body: ArticleWDto = {
  name: 'Savon noir 100 g',
  barcode: null,
  type: 'FINISHED',
  rangeId: '0195f0a0-0000-7000-8000-0000000000a1',
  unit: 'PIECE',
  cost: 3120.5,
  price: 8500,
  threshold: 40,
  supplierId: null,
};

describe('ArticleService', () => {
  let service: ArticleService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ArticleService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('envoie la pagination de la liste, sans critère vide', () => {
    service.list({ page: 2, size: 20, search: '' }).subscribe();

    const request = httpMock.expectOne((req) => req.url === ARTICLES_URL);
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('20');
    expect(request.request.params.has('search')).toBe(false);
    expect(request.request.params.has('type')).toBe(false);
    expect(request.request.params.has('active')).toBe(false);
    request.flush({ ...EMPTY_PAGE, page: 2 });
  });

  it('envoie la recherche, le type, la gamme, le fournisseur et le statut choisis', () => {
    service
      .list({
        page: 1,
        size: 100,
        search: 'savon',
        type: 'RAW_MATERIAL',
        rangeId: 'range-1',
        supplierId: 'supplier-1',
        active: false,
      })
      .subscribe();

    const request = httpMock.expectOne((req) => req.url === ARTICLES_URL);
    expect(request.request.params.get('search')).toBe('savon');
    expect(request.request.params.get('type')).toBe('RAW_MATERIAL');
    expect(request.request.params.get('rangeId')).toBe('range-1');
    expect(request.request.params.get('supplierId')).toBe('supplier-1');
    expect(request.request.params.get('active')).toBe('false');
    request.flush(EMPTY_PAGE);
  });

  it('lit une fiche par son identifiant', () => {
    service.getById('abc').subscribe();

    const request = httpMock.expectOne(`${ARTICLES_URL}/abc`);
    expect(request.request.method).toBe('GET');
    request.flush({});
  });

  it('crée une fiche avec POST', () => {
    service.create(body).subscribe();

    const request = httpMock.expectOne(ARTICLES_URL);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush({});
  });

  it('modifie une fiche avec PUT en renvoyant la version lue', () => {
    service.update('abc', { ...body, version: 3 }).subscribe();

    const request = httpMock.expectOne(`${ARTICLES_URL}/abc`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toMatchObject({ version: 3 });
    request.flush({});
  });

  it('désactive une fiche via PATCH /status, sans suppression', () => {
    service.setActive('abc', false).subscribe();

    const request = httpMock.expectOne(`${ARTICLES_URL}/abc/status`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ active: false });
    request.flush({});
  });
});
