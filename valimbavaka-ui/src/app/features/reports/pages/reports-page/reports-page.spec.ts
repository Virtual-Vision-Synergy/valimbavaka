import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ReportRDto } from '../../data-access/report-models';
import { ReportsPage } from './reports-page';

const REPORTS = '/api/v1/reports';
const REPORT: ReportRDto = {
  granularity: 'MONTH',
  from: '2026-11-01',
  to: '2026-11-30',
  salesChannelId: null,
  rangeId: null,
  sales: { revenue: 25500, salesCount: 2, averageBasket: 12750 },
};
const page = (items: object[]) => ({ items, page: 1, pageSize: 100, totalCount: 1, totalPages: 1 });

/** Rapports (RPT-01) : la synthèse suit la période et les filtres canal et gamme. */
describe('ReportsPage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<ReportsPage>;

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

  afterEach(() => httpMock.verify());

  const change = (select: HTMLSelectElement, value: string): void => {
    select.value = value;
    select.dispatchEvent(new Event('change'));
  };

  it('should show the revenue, the number of sales and the basket of the current month', async () => {
    const element = await open();

    const summary = (element.querySelector('.summary')?.textContent ?? '').replace(/\s+/g, ' ');
    expect(summary).toContain('Du 2026-11-01 au 2026-11-30');
    expect(summary).toContain('25');
    expect(summary).toContain('12');
    expect(element.querySelectorAll('.summary vvs-kpi-card')).toHaveLength(3);
  });

  it('should reload the report with the selected channel and range', async () => {
    const element = await open();
    const [channel, range] = Array.from(element.querySelectorAll('select')) as HTMLSelectElement[];

    change(channel, 'c-fb');
    fixture.detectChanges();
    const byChannel = httpMock.expectOne((req) => req.url === REPORTS);
    expect(byChannel.request.params.get('salesChannelId')).toBe('c-fb');
    expect(byChannel.request.params.has('rangeId')).toBe(false);
    byChannel.flush(REPORT);
    await fixture.whenStable();

    change(range, 'r-bot');
    fixture.detectChanges();
    const both = httpMock.expectOne((req) => req.url === REPORTS);
    expect(both.request.params.get('salesChannelId')).toBe('c-fb');
    expect(both.request.params.get('rangeId')).toBe('r-bot');
    both.flush(REPORT);
  });

  it('should reload the report for a year when the period changes', async () => {
    const element = await open();
    const year = element.querySelector('input[type="radio"][value="YEAR"]') as HTMLInputElement;

    year.click();
    fixture.detectChanges();

    const request = httpMock.expectOne((req) => req.url === REPORTS);
    expect(request.request.params.get('granularity')).toBe('YEAR');
    request.flush({ ...REPORT, granularity: 'YEAR', from: '2026-01-01', to: '2026-12-31' });
  });

  it('should reload the report for the chosen date', async () => {
    const element = await open();
    const date = element.querySelector('input[type="date"]') as HTMLInputElement;

    date.value = '2026-10-15';
    date.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    const request = httpMock.expectOne((req) => req.url === REPORTS);
    expect(request.request.params.get('date')).toBe('2026-10-15');
    request.flush(REPORT);
  });

  it('should clear the channel and range filters on reset', async () => {
    const element = await open();
    const [channel] = Array.from(element.querySelectorAll('select')) as HTMLSelectElement[];
    change(channel, 'c-fb');
    fixture.detectChanges();
    httpMock.expectOne((req) => req.url === REPORTS).flush(REPORT);
    await fixture.whenStable();
    fixture.detectChanges();

    (element.querySelector('button.btn-ghost') as HTMLButtonElement).click();
    fixture.detectChanges();

    const request = httpMock.expectOne((req) => req.url === REPORTS);
    expect(request.request.params.has('salesChannelId')).toBe(false);
    request.flush(REPORT);
  });
});
