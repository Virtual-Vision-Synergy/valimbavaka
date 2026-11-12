import { HttpHeaders, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ToastService } from '../../../shared/components/toast/toast-service';
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
  trend: [{ date: '2026-11-01', revenue: 25500 }],
  ranges: {
    revenue: 25500,
    items: [{ rangeId: 'r-bot', code: 'BOTANIKA', label: 'Botanika', revenue: 25500, share: 100 }],
  },
  ranking: {
    size: 10,
    revenue: 25500,
    items: [
      {
        articleId: 'a-sav',
        code: 'SAV-1',
        name: 'Savon noir',
        quantity: 3,
        revenue: 25500,
        margin: 16138.5,
        share: 100,
      },
    ],
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

  describe('export (RPT-06)', () => {
    let anchorClick: ReturnType<typeof vi.spyOn>;

    beforeEach(() => {
      URL.createObjectURL = vi.fn(() => 'blob:export');
      URL.revokeObjectURL = vi.fn();
      anchorClick = vi
        .spyOn(HTMLAnchorElement.prototype, 'click')
        .mockImplementation(() => undefined);
    });

    afterEach(() => anchorClick.mockRestore());

    it('downloads the xlsx of the displayed report with the same criteria, then confirms it', () => {
      let filename: string | undefined;

      service
        .export(
          { granularity: 'YEAR', date: '2026-11-11', salesChannelId: 'c-1', rangeId: '' },
          'xlsx',
        )
        .subscribe((name) => (filename = name));

      const request = httpMock.expectOne((req) => req.url === '/api/v1/reports/export');
      expect(request.request.params.get('granularity')).toBe('YEAR');
      expect(request.request.params.get('date')).toBe('2026-11-11');
      expect(request.request.params.get('salesChannelId')).toBe('c-1');
      expect(request.request.params.has('rangeId')).toBe(false);
      request.flush(new Blob(['xlsx']), {
        headers: new HttpHeaders({
          'Content-Disposition': 'attachment; filename="rapport-annuel-2026.xlsx"',
        }),
      });
      expect(filename).toBe('rapport-annuel-2026.xlsx');
      expect(anchorClick).toHaveBeenCalledTimes(1);
      expect(TestBed.inject(ToastService).toasts()).toEqual([
        expect.objectContaining({
          messageKey: 'export.done',
          params: { filename: 'rapport-annuel-2026.xlsx' },
        }),
      ]);
    });

    it('calls the PDF route', () => {
      service.export({ granularity: 'DAY', date: '2026-11-12' }, 'pdf').subscribe();

      const request = httpMock.expectOne((req) => req.url === '/api/v1/reports/export/pdf');
      expect(request.request.params.get('granularity')).toBe('DAY');
      request.flush(new Blob(['%PDF']));
      expect(anchorClick).toHaveBeenCalledTimes(1);
    });
  });
});
