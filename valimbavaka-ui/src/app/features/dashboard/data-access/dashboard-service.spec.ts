import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DashboardRDto } from './dashboard-models';
import { DashboardService } from './dashboard-service';
import { DASHBOARD_TEST_VIEW } from './dashboard-testing';

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
});
