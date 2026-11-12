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
  ranking: {
    size: 10,
    revenue: 25500,
    items: [
      {
        articleId: 'a-sav',
        code: 'SAV-1',
        name: 'Savon noir',
        quantity: 2.5,
        revenue: 17000,
        margin: 9200.5,
        share: 66.7,
      },
      {
        articleId: 'a-hui',
        code: 'HUI-1',
        name: 'Huile de coco',
        quantity: 1,
        revenue: 8500,
        margin: null,
        share: 33.3,
      },
    ],
  },
};
const page = (items: object[]) => ({ items, page: 1, pageSize: 100, totalCount: 1, totalPages: 1 });

/** Rapports (RPT-05) : classement des articles les plus vendus, avec quantité, CA, marge et part du CA. */
describe('ReportsPage ranking', () => {
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
    httpMock.expectOne((req) => req.url === '/api/v1/product-ranges').flush(page([]));
    httpMock.expectOne((req) => req.url === REPORTS).flush(REPORT);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  };

  afterEach(() => httpMock.verify());

  const rankingRows = (element: HTMLElement): string[] =>
    Array.from(element.querySelectorAll('.ranking tbody tr')).map((row) =>
      (row.textContent ?? '').replace(/\s+/g, ' ').trim(),
    );

  it('should list the best-selling articles with quantity, revenue, margin and share', async () => {
    const element = await open();

    const rows = rankingRows(element);
    expect(rows).toHaveLength(2);
    expect(rows[0]).toContain('1');
    expect(rows[0]).toContain('Savon noir');
    expect(rows[0]).toContain('2,5');
    expect(rows[0]).toContain('17');
    expect(rows[0]).toContain('9');
    expect(rows[0]).toContain('66,7 %');
    expect(rows[1]).toContain('Huile de coco');
    expect(rows[1]).toContain('33,3 %');
    expect(element.querySelector('[aria-labelledby="top-title"]')?.textContent).not.toContain(
      'NaN',
    );
  });

  it('should show a dash and a spoken explanation when the margin is unknown', async () => {
    const element = await open();

    const cells = Array.from(element.querySelectorAll('.ranking tbody tr:nth-child(2) td'));
    const margin = cells[3];
    expect(margin.textContent).toContain('—');
    expect(margin.querySelector('.sr-only')?.textContent).toContain('Marge inconnue');
  });

  it('should ask the ranking again with the filters and show an empty state when nothing sold', async () => {
    const element = await open();
    const [channel] = Array.from(element.querySelectorAll('select')) as HTMLSelectElement[];

    channel.value = 'c-fb';
    channel.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    const request = httpMock.expectOne((req) => req.url === REPORTS);
    expect(request.request.params.get('salesChannelId')).toBe('c-fb');
    request.flush({ ...REPORT, ranking: { size: 10, revenue: 0, items: [] } });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('.ranking')).toBeNull();
    const section = element.querySelector('[aria-labelledby="top-title"]');
    expect(section?.textContent).toContain('Aucun article vendu sur la période');
  });

  it('should not keep any demonstration data in the ranking', async () => {
    const element = await open();

    expect(element.textContent).not.toContain('Gommage café');
  });
});
