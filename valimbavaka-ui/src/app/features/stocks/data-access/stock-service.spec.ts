import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { StockRowRDto } from './stock-models';
import { StockService } from './stock-service';

const page = (items: unknown[]) => ({
  items,
  page: 1,
  pageSize: 100,
  totalCount: items.length,
  totalPages: 1,
});

describe('StockService', () => {
  let service: StockService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(StockService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lit les stocks du serveur, triés par nom, en lecture seule', () => {
    let rows: readonly StockRowRDto[] = [];
    service.list().subscribe((value) => (rows = value));

    const stocks = httpMock.expectOne((req) => req.url === '/api/v1/stocks');
    expect(stocks.request.method).toBe('GET');
    expect(stocks.request.params.get('size')).toBe('100');
    expect(stocks.request.params.get('sortBy')).toBe('name');
    stocks.flush(
      page([
        {
          articleId: 'art-1',
          code: 'ART-0031',
          name: 'Savon noir 100 g',
          rangeId: 'range-1',
          unit: 'PIECE',
          threshold: 40,
          cost: null,
          total: 12,
          locations: [{ locationId: 'l1', code: 'STORE', label: 'Boutique', quantity: 12 }],
        },
      ]),
    );
    httpMock
      .expectOne((req) => req.url === '/api/v1/product-ranges')
      .flush(page([{ id: 'range-1', code: 'ZEZIKA', label: 'Zezika' }]));

    expect(rows).toHaveLength(1);
    expect(rows[0].quantities.SHOP).toBe(12);
    expect(rows[0].range).toBe('ZEZIKA');
  });

  it('renvoie une liste vide quand aucun article n’est actif', () => {
    let rows: readonly StockRowRDto[] | undefined;
    service.list().subscribe((value) => (rows = value));

    httpMock.expectOne((req) => req.url === '/api/v1/stocks').flush(page([]));
    httpMock.expectOne((req) => req.url === '/api/v1/product-ranges').flush(page([]));

    expect(rows).toEqual([]);
  });
});
