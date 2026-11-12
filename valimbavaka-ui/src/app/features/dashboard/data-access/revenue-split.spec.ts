import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DashboardService } from './dashboard-service';
import { RevenueSplitRDto } from './revenue-split-models';
import { REVENUE_SPLIT_TEST_VIEW } from './revenue-split-testing';

describe('DashboardService.revenueSplit', () => {
  let httpMock: HttpTestingController;
  let service: DashboardService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    service = TestBed.inject(DashboardService);
  });

  afterEach(() => httpMock.verify());

  it('should read the split by range and channel from its own endpoint', () => {
    let received: RevenueSplitRDto | undefined;

    service.revenueSplit().subscribe((split) => (received = split));
    const request = httpMock.expectOne('/api/v1/dashboard/revenue-split');
    request.flush(REVENUE_SPLIT_TEST_VIEW);

    expect(request.request.method).toBe('GET');
    expect(received?.byRange).toHaveLength(3);
    expect(received?.byChannel[2]).toEqual({
      salesChannelId: 'c-3',
      label: null,
      revenue: 218_000,
      sharePercent: 17,
    });
  });
});
