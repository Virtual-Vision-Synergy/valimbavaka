import { HttpHeaders, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { ReportRDto } from '../../data-access/report-models';
import { ReportsPage } from './reports-page';

const REPORTS = '/api/v1/reports';
const SALES = { revenue: 25500, salesCount: 2, averageBasket: 12750 };
const REPORT: ReportRDto = {
  granularity: 'MONTH',
  from: '2026-11-01',
  to: '2026-11-30',
  salesChannelId: null,
  rangeId: null,
  sales: SALES,
  comparison: {
    from: '2026-10-01',
    to: '2026-10-31',
    sales: SALES,
    variation: { revenue: null, salesCount: null, averageBasket: null },
  },
  trend: [{ date: '2026-11-01', revenue: 25500 }],
  ranges: { revenue: 0, items: [] },
  ranking: { size: 10, revenue: 0, items: [] },
};
const page = (items: object[]) => ({ items, page: 1, pageSize: 100, totalCount: 1, totalPages: 1 });

/** Rapports (RPT-06) : les boutons PDF et XLSX téléchargent le rapport affiché et confirment le fichier. */
describe('ReportsPage export', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<ReportsPage>;
  let anchorClick: ReturnType<typeof vi.spyOn>;

  const open = async (): Promise<HTMLElement> => {
    await TestBed.configureTestingModule({
      imports: [ReportsPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTestTranslations(),
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
    fixture = TestBed.createComponent(ReportsPage);
    fixture.detectChanges();
    httpMock
      .expectOne((req) => req.url === '/api/v1/sales-channels')
      .flush(page([{ id: 'c-fb', code: 'FACEBOOK', label: 'Facebook' }]));
    httpMock
      .expectOne((req) => req.url === '/api/v1/product-ranges')
      .flush(page([{ id: 'r-bot', code: 'BOTANIKA', label: 'Botanika' }]));
    httpMock.expectOne((req) => req.url === REPORTS).flush(REPORT);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  };

  const change = (select: HTMLSelectElement, value: string): void => {
    select.value = value;
    select.dispatchEvent(new Event('change'));
  };

  const exportButtons = (): NodeListOf<HTMLButtonElement> =>
    (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>(
      'vvs-page-header button',
    );

  beforeEach(() => {
    URL.createObjectURL = vi.fn(() => 'blob:export');
    URL.revokeObjectURL = vi.fn();
    anchorClick = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);
  });

  afterEach(() => {
    httpMock.verify();
    anchorClick.mockRestore();
  });

  it('should download the PDF of the displayed report and confirm the file', async () => {
    await open();

    exportButtons()[0].click();

    const request = httpMock.expectOne((req) => req.url === `${REPORTS}/export/pdf`);
    expect(request.request.params.get('granularity')).toBe('MONTH');
    expect(request.request.params.has('salesChannelId')).toBe(false);
    request.flush(new Blob(['%PDF']), {
      headers: new HttpHeaders({
        'Content-Disposition': 'attachment; filename="rapport-mensuel-2026-11.pdf"',
      }),
    });
    expect(anchorClick).toHaveBeenCalledTimes(1);
    expect(TestBed.inject(ToastService).toasts()).toEqual([
      expect.objectContaining({
        messageKey: 'export.done',
        params: { filename: 'rapport-mensuel-2026-11.pdf' },
      }),
    ]);
  });

  it('should export the XLSX with the channel and the range currently selected', async () => {
    const element = await open();
    const [channel, range] = Array.from(element.querySelectorAll('select')) as HTMLSelectElement[];
    change(channel, 'c-fb');
    fixture.detectChanges();
    httpMock.expectOne((req) => req.url === REPORTS).flush(REPORT);
    await fixture.whenStable();
    change(range, 'r-bot');
    fixture.detectChanges();
    httpMock.expectOne((req) => req.url === REPORTS).flush(REPORT);
    await fixture.whenStable();
    fixture.detectChanges();

    exportButtons()[1].click();

    const request = httpMock.expectOne((req) => req.url === `${REPORTS}/export`);
    expect(request.request.params.get('salesChannelId')).toBe('c-fb');
    expect(request.request.params.get('rangeId')).toBe('r-bot');
    request.flush(new Blob(['xlsx']));
    expect(anchorClick).toHaveBeenCalledTimes(1);
  });

  it('should tell the user when the export fails and let her try again', async () => {
    await open();

    exportButtons()[1].click();
    httpMock
      .expectOne((req) => req.url === `${REPORTS}/export`)
      .flush(new Blob(['{}']), { status: 403, statusText: 'Forbidden' });
    fixture.detectChanges();

    expect(anchorClick).not.toHaveBeenCalled();
    expect(TestBed.inject(ToastService).toasts()).toEqual([
      expect.objectContaining({ messageKey: 'export.forbidden', tone: 'danger' }),
    ]);
    expect(exportButtons()[1].disabled).toBe(false);
  });
});
