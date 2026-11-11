import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ReportRDto } from './report-models';
import { ReportService } from './report-service';

const REPORT: ReportRDto = {
  granularity: 'MONTH',
  from: '2026-11-01',
  to: '2026-11-30',
  salesChannelId: null,
  rangeId: null,
  sales: { revenue: 25500, salesCount: 2, averageBasket: 12750 },
  comparison: {
    from: '2026-10-01',
    to: '2026-10-31',
    sales: { revenue: 0, salesCount: 0, averageBasket: 0 },
    variation: { revenue: null, salesCount: null, averageBasket: null },
  },
};

/** Rapports (RPT-01) : `GET /api/v1/reports` avec la période et les filtres canal et gamme. */
describe('ReportService', () => {
  let service: ReportService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ReportService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('sends the granularity, the date and the filters as query parameters', () => {
    let received: ReportRDto | undefined;
    service
      .get({ granularity: 'YEAR', date: '2026-11-11', salesChannelId: 'c-1', rangeId: 'r-1' })
      .subscribe((report) => (received = report));

    const request = httpMock.expectOne((req) => req.url === '/api/v1/reports');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('granularity')).toBe('YEAR');
    expect(request.request.params.get('date')).toBe('2026-11-11');
    expect(request.request.params.get('salesChannelId')).toBe('c-1');
    expect(request.request.params.get('rangeId')).toBe('r-1');
    request.flush(REPORT);
    expect(received).toEqual(REPORT);
  });

  it('omits empty filters so that the server applies its defaults', () => {
    service.get({ granularity: 'MONTH', salesChannelId: '', rangeId: '' }).subscribe();

    const request = httpMock.expectOne((req) => req.url === '/api/v1/reports');
    expect(request.request.params.has('salesChannelId')).toBe(false);
    expect(request.request.params.has('rangeId')).toBe(false);
    expect(request.request.params.has('date')).toBe(false);
    request.flush(REPORT);
  });
});
