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
  comparison: {
    from: '2026-10-01',
    to: '2026-10-31',
    sales: { revenue: 20000, salesCount: 4, averageBasket: 5000 },
    variation: { revenue: 27.5, salesCount: -50, averageBasket: 155 },
  },
  trend: [
    { date: '2026-11-01', revenue: 0 },
    { date: '2026-11-02', revenue: 8500 },
    { date: '2026-11-03', revenue: 17000 },
  ],
  ranges: {
    revenue: 25500,
    items: [
      { rangeId: 'r-zez', code: 'ZEZIKA', label: 'Zezika', revenue: 17000, share: 66.7 },
      { rangeId: 'r-bot', code: 'BOTANIKA', label: 'Botanika', revenue: 8500, share: 33.3 },
    ],
  },
  ranking: { size: 10, revenue: 0, items: [] },
};
const page = (items: object[]) => ({ items, page: 1, pageSize: 100, totalCount: 1, totalPages: 1 });

/** Rapports (RPT-01 à RPT-04) : synthèse, comparaison, courbe et gammes suivent la période et les filtres. */
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

  it('should show each indicator with its variation in percent against the previous period', async () => {
    const element = await open();

    const badges = Array.from(
      element.querySelectorAll('.summary vvs-kpi-card vvs-status-badge'),
    ).map((badge) => (badge.textContent ?? '').trim());
    expect(badges).toEqual(['+27,5 %', '−50,0 %', '+155,0 %']);
    const summary = (element.querySelector('.summary')?.textContent ?? '').replace(/\s+/g, ' ');
    expect(summary).toContain('du 2026-10-01 au 2026-10-31');
    expect(element.querySelectorAll('.summary .vs')).toHaveLength(3);
  });

  it('should not show a percentage when the previous period has no sale', async () => {
    const element = await open();
    const [channel] = Array.from(element.querySelectorAll('select')) as HTMLSelectElement[];

    change(channel, 'c-fb');
    fixture.detectChanges();
    httpMock
      .expectOne((req) => req.url === REPORTS)
      .flush({
        ...REPORT,
        comparison: {
          ...REPORT.comparison,
          sales: { revenue: 0, salesCount: 0, averageBasket: 0 },
          variation: { revenue: null, salesCount: null, averageBasket: null },
        },
      });
    await fixture.whenStable();
    fixture.detectChanges();

    const badges = Array.from(
      element.querySelectorAll('.summary vvs-kpi-card vvs-status-badge'),
    ).map((badge) => (badge.textContent ?? '').trim());
    expect(badges).toEqual(['n.d.', 'n.d.', 'n.d.']);
    expect(element.querySelector('.summary')?.textContent).not.toContain('NaN');
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

  it('should draw one bar per day of the period, a day without sale being an empty bar', async () => {
    const element = await open();

    const columns = element.querySelectorAll('.chart .column');
    expect(columns).toHaveLength(3);
    const heights = Array.from(element.querySelectorAll<HTMLElement>('.chart .bar')).map(
      (bar) => bar.style.height,
    );
    expect(heights).toEqual(['0%', '42.5%', '85%']);
    const rows = element.querySelectorAll('.chart table tbody tr');
    expect(rows).toHaveLength(3);
    expect(rows[2].textContent).toContain('17');
  });

  it('should draw one bar per month for a year, with a month column in the data table', async () => {
    const element = await open();
    const year = element.querySelector('input[type="radio"][value="YEAR"]') as HTMLInputElement;
    year.click();
    fixture.detectChanges();

    httpMock
      .expectOne((req) => req.url === REPORTS)
      .flush({
        ...REPORT,
        granularity: 'YEAR',
        from: '2026-01-01',
        to: '2026-12-31',
        trend: Array.from({ length: 12 }, (_, index) => ({
          date: `2026-${String(index + 1).padStart(2, '0')}-01`,
          revenue: index === 10 ? 25500 : 0,
        })),
      });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelectorAll('.chart .column')).toHaveLength(12);
    expect(element.querySelector('.chart thead th')?.textContent).toContain('Mois');
  });

  it('should list each range with its revenue and its share, the biggest first', async () => {
    const element = await open();

    const rows = Array.from(element.querySelectorAll('.legend li')).map((row) =>
      (row.textContent ?? '').replace(/\s+/g, ' ').trim(),
    );
    expect(rows).toHaveLength(2);
    expect(rows[0]).toContain('Zezika');
    expect(rows[0]).toContain('66,7 %');
    expect(rows[1]).toContain('Botanika');
    expect(rows[1]).toContain('33,3 %');
    expect(element.querySelectorAll('circle.slice')).toHaveLength(2);
    expect(element.querySelector('.legend li')?.getAttribute('data-range')).toBe('ZEZIKA');
  });

  it('should show an empty state and no ring when no range sold anything', async () => {
    const element = await open();
    const [channel] = Array.from(element.querySelectorAll('select')) as HTMLSelectElement[];

    change(channel, 'c-fb');
    fixture.detectChanges();
    httpMock
      .expectOne((req) => req.url === REPORTS)
      .flush({ ...REPORT, ranges: { revenue: 0, items: [] } });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('vvs-range-donut')).toBeNull();
    const section = element.querySelector('[aria-labelledby="range-title"]');
    expect(section?.textContent).toContain('Aucune vente sur la période');
    expect(section?.textContent).not.toContain('NaN');
  });

  it('should say that the range filter does not apply to the split', async () => {
    const element = await open();
    const [, range] = Array.from(element.querySelectorAll('select')) as HTMLSelectElement[];

    change(range, 'r-bot');
    fixture.detectChanges();
    httpMock.expectOne((req) => req.url === REPORTS).flush({ ...REPORT, rangeId: 'r-bot' });
    await fixture.whenStable();
    fixture.detectChanges();

    const section = element.querySelector('[aria-labelledby="range-title"]');
    expect(section?.textContent).toContain("Le filtre gamme ne s'applique pas ici");
    expect(element.querySelectorAll('.legend li')).toHaveLength(2);
  });

  it('should explain the gap when the sale discounts make the summary lower than the ranges', async () => {
    const element = await open();
    const [channel] = Array.from(element.querySelectorAll('select')) as HTMLSelectElement[];

    change(channel, 'c-fb');
    fixture.detectChanges();
    httpMock
      .expectOne((req) => req.url === REPORTS)
      .flush({ ...REPORT, sales: { ...REPORT.sales, revenue: 25000 } });
    await fixture.whenStable();
    fixture.detectChanges();

    const section = (
      element.querySelector('[aria-labelledby="range-title"]')?.textContent ?? ''
    ).replace(/\s+/g, ' ');
    expect(section).toContain('Écart avec le CA de la synthèse');
    expect(section).toContain('500');
  });

  it('should not mention a gap when the ranges add up to the summary', async () => {
    const element = await open();

    expect(element.querySelector('[aria-labelledby="range-title"] .note')).toBeNull();
  });
});
