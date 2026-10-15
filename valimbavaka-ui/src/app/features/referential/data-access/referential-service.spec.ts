import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SalesChannelRDto } from './referential-models';
import { ReferentialService } from './referential-service';

describe('ReferentialService', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should list sales channels sorted by label', () => {
    const channels: SalesChannelRDto[] = [{ id: '1', code: 'BOUTIQUE', label: 'Boutique' }];
    let result: SalesChannelRDto[] | undefined;
    TestBed.inject(ReferentialService)
      .listSalesChannels()
      .subscribe((items) => (result = items));

    const request = httpMock.expectOne((req) => req.url === '/api/v1/sales-channels');
    expect(request.request.params.get('sortBy')).toBe('label');
    request.flush({ items: channels, page: 1, pageSize: 100, totalCount: 1, totalPages: 1 });

    expect(result).toEqual(channels);
  });

  it('should call the dedicated endpoints for ranges and stock locations', () => {
    const service = TestBed.inject(ReferentialService);
    service.listProductRanges().subscribe();
    service.listStockLocations().subscribe();

    const empty = { items: [], page: 1, pageSize: 100, totalCount: 0, totalPages: 0 };
    httpMock.expectOne((req) => req.url === '/api/v1/product-ranges').flush(empty);
    httpMock.expectOne((req) => req.url === '/api/v1/stock-locations').flush(empty);
  });
});
