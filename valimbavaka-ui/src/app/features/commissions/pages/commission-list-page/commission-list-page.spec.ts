import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { PageResponse } from '../../../../shared/models/page-response';
import { CommissionRDto, SellerCommissionRDto } from '../../data-access/commission-models';
import { CommissionListPage, monthKey, monthRange } from './commission-list-page';

const COMMISSIONS_URL = '/api/v1/commissions';
const SELLERS_URL = `${COMMISSIONS_URL}/sellers`;
const VALIDATION_URL = `${COMMISSIONS_URL}/validation`;

const seller = (overrides: Partial<SellerCommissionRDto> = {}): SellerCommissionRDto => ({
  sellerId: 'u1',
  sellerName: 'Miora Rakoto',
  status: 'TO_VALIDATE',
  toValidateCount: 2,
  toValidateAmount: 850,
  validatedCount: 0,
  validatedAmount: 0,
  ...overrides,
});

const commission = (overrides: Partial<CommissionRDto> = {}): CommissionRDto => ({
  id: 'c1',
  sourceType: 'SALE',
  sourceId: 's1',
  sourceNumber: 'VTE-0042',
  sellerId: 'u1',
  sellerName: 'Miora Rakoto',
  salesChannelId: 'ch1',
  salonServiceId: null,
  businessDate: '2026-11-08',
  revenue: 25500,
  baseAmount: 25500,
  base: 'REVENUE',
  rate: 4,
  rateSource: 'SALES_CHANNEL',
  ruleId: 'r1',
  amount: 1020,
  status: 'CALCULATED',
  createdAt: '2026-11-08T08:30:00Z',
  ...overrides,
});

const pageOf = <T>(items: T[]): PageResponse<T> => ({
  items,
  page: 1,
  pageSize: 20,
  totalCount: items.length,
  totalPages: 1,
});

describe('monthKey et monthRange', () => {
  it('should give the month key with an offset and the first and last days of the month', () => {
    expect(monthKey(new Date(2026, 0, 31), -1)).toBe('2025-12');
    expect(monthRange('2026-11')).toEqual({ from: '2026-11-01', to: '2026-11-30' });
    expect(monthRange('2028-02')).toEqual({ from: '2028-02-01', to: '2028-02-29' });
  });
});

describe('CommissionListPage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<CommissionListPage>;
  let element: HTMLElement;

  const isListRequest = (req: { url: string }): boolean => req.url === COMMISSIONS_URL;
  const isSellersRequest = (req: { url: string }): boolean => req.url === SELLERS_URL;
  const sellerRows = (): NodeListOf<Element> =>
    element.querySelectorAll('section table')[0].querySelectorAll('tbody tr');

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommissionListPage],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CommissionListPage);
    element = fixture.nativeElement as HTMLElement;
  });

  /** Réponse des vendeuses, demandées à la création de la page. */
  const flushSellers = async (items: SellerCommissionRDto[] = [seller()]): Promise<void> => {
    httpMock.expectOne(isSellersRequest).flush(pageOf(items));
    await fixture.whenStable();
  };

  afterEach(() => httpMock.verify());

  it('should request the current month, newest first', () => {
    const request = httpMock.expectOne(isListRequest);

    const { from, to } = monthRange(monthKey(new Date(), 0));
    expect(request.request.params.get('from')).toBe(from);
    expect(request.request.params.get('to')).toBe(to);
    expect(request.request.params.get('sortBy')).toBe('createdAt');
    expect(request.request.params.get('sortDirection')).toBe('DESC');
    request.flush(pageOf([]));
    httpMock.expectOne(isSellersRequest).flush(pageOf([]));
  });

  it('should show the operation, the seller, the base, the rate with its origin and the commission', async () => {
    httpMock.expectOne(isListRequest).flush(
      pageOf([
        commission(),
        commission({
          id: 'c2',
          sourceType: 'SERVICE_RECORD',
          sourceNumber: 'PRE-0007',
          base: 'MARGIN',
          rate: 10,
          rateSource: 'SALON_SERVICE',
          amount: 3500,
        }),
      ]),
    );
    await flushSellers();

    const rows = element.querySelectorAll('tbody')[1].querySelectorAll('tr');
    expect(rows).toHaveLength(2);
    expect(rows[0].textContent).toContain('VTE-0042');
    expect(rows[0].textContent).toContain('Vente');
    expect(rows[0].textContent).toContain('Miora Rakoto');
    expect(rows[0].textContent).toContain("Chiffre d'affaires");
    expect(rows[0].textContent).toContain('4 %');
    expect(rows[0].textContent).toContain('Règle du canal');
    expect(rows[0].textContent).toMatch(/1\s?020/);
    expect(rows[1].textContent).toContain('Prestation salon');
    expect(rows[1].textContent).toContain('Marge brute');
    expect(rows[1].textContent).toContain('Service salon');
    expect(rows[1].textContent).toMatch(/3\s?500/);
    expect(element.textContent).toContain('2 commission(s)');
  });

  it('should mark the commission of a cancelled sale and only that one', async () => {
    httpMock
      .expectOne(isListRequest)
      .flush(pageOf([commission(), commission({ id: 'c2', status: 'CANCELLED' })]));
    await flushSellers();

    const rows = element.querySelectorAll('tbody')[1].querySelectorAll('tr');
    expect(rows[0].textContent).not.toContain('Annulée avec la vente');
    expect(rows[1].textContent).toContain('Annulée avec la vente');
  });

  it('should reload the chosen month from its first page', async () => {
    httpMock.expectOne(isListRequest).flush(pageOf([commission()]));
    await flushSellers();
    const select = element.querySelector('select') as HTMLSelectElement;
    const previous = monthKey(new Date(), -1);

    select.value = previous;
    select.dispatchEvent(new Event('change'));

    const request = httpMock.expectOne(isListRequest);
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('from')).toBe(monthRange(previous).from);
    const sellersRequest = httpMock.expectOne(isSellersRequest);
    expect(sellersRequest.request.params.get('from')).toBe(monthRange(previous).from);
    request.flush(pageOf([]));
    sellersRequest.flush(pageOf([]));
    await fixture.whenStable();
    expect(element.textContent).toContain('Aucune commission ce mois-ci');
  });

  it('should show the status to validate or validated of each seller, with the totals', async () => {
    httpMock.expectOne(isListRequest).flush(pageOf([]));
    await flushSellers([
      seller(),
      seller({
        sellerId: 'u2',
        sellerName: 'Hanta Razafy',
        status: 'VALIDATED',
        toValidateCount: 0,
        toValidateAmount: 0,
        validatedCount: 3,
        validatedAmount: 1275,
      }),
    ]);

    const rows = sellerRows();
    expect(rows).toHaveLength(2);
    expect(rows[0].textContent).toContain('Miora Rakoto');
    expect(rows[0].textContent).toContain('À valider');
    expect(rows[0].textContent).toMatch(/850/);
    expect(rows[0].querySelector('button')).not.toBeNull();
    expect(rows[1].textContent).toContain('Hanta Razafy');
    expect(rows[1].textContent).toContain('Validée');
    expect(rows[1].textContent).toMatch(/1\s?275/);
    expect(rows[1].querySelector('button')).toBeNull();
  });

  it('should validate the seller of the month only after the confirmation, then reload', async () => {
    httpMock.expectOne(isListRequest).flush(pageOf([commission()]));
    await flushSellers();

    (sellerRows()[0].querySelector('button') as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(element.textContent).toContain('Une commission validée est définitive');
    httpMock.expectNone(VALIDATION_URL);
    (element.querySelector('dialog .btn-primary') as HTMLButtonElement).click();

    const request = httpMock.expectOne(VALIDATION_URL);
    const { from, to } = monthRange(monthKey(new Date(), 0));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ sellerId: 'u1', from, to });
    request.flush({
      sellerId: 'u1',
      sellerName: 'Miora Rakoto',
      from,
      to,
      validatedCount: 2,
      validatedAmount: 850,
    });
    httpMock.expectOne(isSellersRequest).flush(
      pageOf([
        seller({
          status: 'VALIDATED',
          toValidateCount: 0,
          toValidateAmount: 0,
          validatedCount: 2,
          validatedAmount: 850,
        }),
      ]),
    );
    httpMock.expectOne(isListRequest).flush(pageOf([commission({ status: 'VALIDATED' })]));
    await fixture.whenStable();

    expect(element.textContent).toContain('2 commission(s) de Miora Rakoto validée(s)');
    expect(sellerRows()[0].textContent).toContain('Validée');
    expect(sellerRows()[0].querySelector('button')).toBeNull();
  });

  it('should show the server error and refresh when the commissions are already validated', async () => {
    httpMock.expectOne(isListRequest).flush(pageOf([]));
    await flushSellers();

    (sellerRows()[0].querySelector('button') as HTMLButtonElement).click();
    await fixture.whenStable();
    (element.querySelector('dialog .btn-primary') as HTMLButtonElement).click();
    httpMock.expectOne(VALIDATION_URL).flush(
      {
        status: 409,
        code: 'CONFLICT',
        detail: 'Les commissions de cette vendeuse sont déjà validées sur cette période.',
      },
      { status: 409, statusText: 'Conflict' },
    );
    httpMock.expectOne(isSellersRequest).flush(pageOf([seller({ status: 'VALIDATED' })]));
    await fixture.whenStable();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('déjà validées');
  });

  it('should send nothing when the confirmation is cancelled', async () => {
    httpMock.expectOne(isListRequest).flush(pageOf([]));
    await flushSellers();

    (sellerRows()[0].querySelector('button') as HTMLButtonElement).click();
    await fixture.whenStable();
    (element.querySelector('dialog .btn-secondary') as HTMLButtonElement).click();
    await fixture.whenStable();

    httpMock.expectNone(VALIDATION_URL);
    expect(element.querySelector('dialog')).toBeNull();
  });
});
