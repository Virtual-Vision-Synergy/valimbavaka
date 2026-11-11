import { HttpHeaders, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { PageResponse } from '../../../../shared/models/page-response';
import { CommissionRDto, SellerCommissionRDto } from '../../data-access/commission-models';
import { CommissionListPage, monthKey, monthRange } from './commission-list-page';

const COMMISSIONS_URL = '/api/v1/commissions';
const SELLERS_URL = `${COMMISSIONS_URL}/sellers`;
const RECALCULATION_URL = `${COMMISSIONS_URL}/recalculation`;
const EXPORT_URL = `${COMMISSIONS_URL}/export`;

const seller = (): SellerCommissionRDto => ({
  sellerId: 'u1',
  sellerName: 'Miora Rakoto',
  status: 'TO_VALIDATE',
  toValidateCount: 2,
  toValidateAmount: 850,
  validatedCount: 0,
  validatedAmount: 0,
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

/** COM-05 : recalcul de la période (après confirmation) et export du mois, avec confirmation d'exécution. */
describe('CommissionListPage : recalcul et export', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<CommissionListPage>;
  let element: HTMLElement;
  let anchorClick: ReturnType<typeof vi.spyOn>;

  const isListRequest = (req: { url: string }): boolean => req.url === COMMISSIONS_URL;
  const isSellersRequest = (req: { url: string }): boolean => req.url === SELLERS_URL;
  const actionButtons = (): NodeListOf<HTMLButtonElement> =>
    element.querySelectorAll<HTMLButtonElement>('.actions button');

  const openPage = async (): Promise<void> => {
    httpMock.expectOne(isListRequest).flush(pageOf([commission()]));
    httpMock.expectOne(isSellersRequest).flush(pageOf([seller()]));
    await fixture.whenStable();
  };

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

  it('should recalculate the displayed month for all sellers only after the confirmation, then say what changed', async () => {
    await openPage();

    actionButtons()[0].click();
    await fixture.whenStable();
    expect(element.textContent).toContain(
      'Les commissions validées et annulées ne sont pas modifiées',
    );
    httpMock.expectNone(RECALCULATION_URL);
    (element.querySelector('dialog .btn-primary') as HTMLButtonElement).click();

    const request = httpMock.expectOne(RECALCULATION_URL);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(monthRange(monthKey(new Date(), 0)));
    request.flush({
      ...monthRange(monthKey(new Date(), 0)),
      sellerId: null,
      updatedCount: 3,
      unchangedCount: 1,
      validatedCount: 2,
      previousAmount: 1000,
      newAmount: 1200,
    });
    httpMock.expectOne(isSellersRequest).flush(pageOf([seller()]));
    httpMock.expectOne(isListRequest).flush(pageOf([commission({ amount: 1200 })]));
    await fixture.whenStable();

    const notice = element.querySelector('[role="status"].alert-success')?.textContent ?? '';
    expect(notice).toContain('3 commission(s) mise(s) à jour');
    expect(notice).toContain('1 inchangée(s)');
    expect(notice).toContain('2 validée(s) laissée(s) telle(s) quelle(s)');
  });

  it('should send nothing when the recalculation is cancelled', async () => {
    await openPage();

    actionButtons()[0].click();
    await fixture.whenStable();
    (element.querySelector('dialog .btn-secondary') as HTMLButtonElement).click();
    await fixture.whenStable();

    httpMock.expectNone(RECALCULATION_URL);
    expect(element.querySelector('dialog')).toBeNull();
  });

  it('should show the server error when the recalculation is refused', async () => {
    await openPage();
    actionButtons()[0].click();
    await fixture.whenStable();
    (element.querySelector('dialog .btn-primary') as HTMLButtonElement).click();

    httpMock
      .expectOne(RECALCULATION_URL)
      .flush(
        { status: 422, code: 'VALIDATION_FAILED', detail: 'La période est trop longue.' },
        { status: 422, statusText: 'Unprocessable Content' },
      );
    await fixture.whenStable();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('trop longue');
    expect(actionButtons()[0].disabled).toBe(false);
  });

  it('should export the displayed month as xlsx and confirm the file', async () => {
    await openPage();

    actionButtons()[1].click();

    const request = httpMock.expectOne((req) => req.url === EXPORT_URL);
    const { from, to } = monthRange(monthKey(new Date(), 0));
    expect(request.request.params.get('from')).toBe(from);
    expect(request.request.params.get('to')).toBe(to);
    request.flush(new Blob(['xlsx']), {
      headers: new HttpHeaders({
        'Content-Disposition': 'attachment; filename="commissions.xlsx"',
      }),
    });
    expect(anchorClick).toHaveBeenCalledTimes(1);
    expect(TestBed.inject(ToastService).toasts()).toEqual([
      expect.objectContaining({
        messageKey: 'export.done',
        params: { filename: 'commissions.xlsx' },
      }),
    ]);
  });

  it('should export the displayed month as PDF', async () => {
    await openPage();

    actionButtons()[2].click();

    const request = httpMock.expectOne((req) => req.url === `${EXPORT_URL}/pdf`);
    expect(request.request.params.get('from')).toBe(monthRange(monthKey(new Date(), 0)).from);
    request.flush(new Blob(['%PDF']));
    expect(anchorClick).toHaveBeenCalledTimes(1);
  });

  it('should tell the administrator when the export fails', async () => {
    await openPage();

    actionButtons()[1].click();
    httpMock
      .expectOne((req) => req.url === EXPORT_URL)
      .flush(new Blob(['{}']), { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(anchorClick).not.toHaveBeenCalled();
    expect(TestBed.inject(ToastService).toasts()).toEqual([
      expect.objectContaining({ messageKey: 'export.failed', tone: 'danger' }),
    ]);
    expect(actionButtons()[1].disabled).toBe(false);
  });
});
