import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DashboardRDto, RevenueTrendRDto } from './dashboard-models';
import { DashboardService } from './dashboard-service';
import { DASHBOARD_TEST_VIEW, REVENUE_TREND_TEST_VIEW } from './dashboard-testing';

describe('DashboardService', () => {
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

  it('should read the four indicators of the day from the API', () => {
    let received: DashboardRDto | undefined;

    service.get().subscribe((view) => (received = view));
    const request = httpMock.expectOne('/api/v1/dashboard');
    request.flush(DASHBOARD_TEST_VIEW);

    expect(request.request.method).toBe('GET');
    expect(received).toEqual(DASHBOARD_TEST_VIEW);
  });

  it('should keep a day not opened yet as null status and null stock gap', () => {
    let received: DashboardRDto | undefined;
    const closed: DashboardRDto = { ...DASHBOARD_TEST_VIEW, status: null, stockGap: null };

    service.get().subscribe((view) => (received = view));
    httpMock.expectOne('/api/v1/dashboard').flush(closed);

    expect(received?.status).toBeNull();
    expect(received?.stockGap).toBeNull();
  });

  it('should read the revenue of the last seven days from its own endpoint', () => {
    let received: RevenueTrendRDto | undefined;

    service.revenueTrend().subscribe((trend) => (received = trend));
    const request = httpMock.expectOne('/api/v1/dashboard/revenue-trend');
    request.flush(REVENUE_TREND_TEST_VIEW);

    expect(request.request.method).toBe('GET');
    expect(received?.points).toHaveLength(7);
    expect(received?.points[3]).toEqual({ date: '2026-11-08', revenue: 0, salesCount: 0 });
  });
});
