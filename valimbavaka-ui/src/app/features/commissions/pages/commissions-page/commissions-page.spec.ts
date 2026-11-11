import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { PageResponse } from '../../../../shared/models/page-response';
import { CommissionFollowUpRDto, CommissionRDto } from '../../data-access/commission-models';
import { CommissionsPage } from './commissions-page';

const FOLLOW_UP_URL = '/api/v1/commissions/follow-up';
const LINES_URL = `${FOLLOW_UP_URL}/lines`;

const followUp = (overrides: Partial<CommissionFollowUpRDto> = {}): CommissionFollowUpRDto => ({
  sellerId: 'u1',
  sellerName: 'Miora Rakoto',
  month: '2026-11',
  from: '2026-11-01',
  to: '2026-11-30',
  commissionCount: 2,
  revenue: 17000,
  commissionAmount: 850,
  toValidateAmount: 425,
  validatedAmount: 425,
  monthlyTarget: 20000,
  targetProgressPercent: 85,
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
  revenue: 8500,
  baseAmount: 8500,
  base: 'REVENUE',
  rate: 5,
  rateSource: 'SELLER',
  ruleId: null,
  amount: 425,
  status: 'CALCULATED',
  createdAt: '2026-11-08T08:30:00Z',
  ...overrides,
});

const pageOf = (items: CommissionRDto[]): PageResponse<CommissionRDto> => ({
  items,
  page: 1,
  pageSize: 20,
  totalCount: items.length,
  totalPages: 1,
});

const isFollowUp = (req: { url: string }): boolean => req.url === FOLLOW_UP_URL;
const isLines = (req: { url: string }): boolean => req.url === LINES_URL;

describe('CommissionsPage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<CommissionsPage>;
  let element: HTMLElement;

  const open = async (role: 'SELLER' | 'ADMIN', sellerId?: string): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [CommissionsPage],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser(role));
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CommissionsPage);
    element = fixture.nativeElement as HTMLElement;
    if (sellerId) {
      fixture.componentRef.setInput('sellerId', sellerId);
    }
    fixture.detectChanges();
  };

  const answer = async (
    summary: CommissionFollowUpRDto,
    items: CommissionRDto[] = [commission()],
  ): Promise<void> => {
    httpMock.expectOne(isFollowUp).flush(summary);
    httpMock.expectOne(isLines).flush(pageOf(items));
    await fixture.whenStable();
    fixture.detectChanges();
  };

  const text = (): string => (element.textContent ?? '').replace(/\s/g, '');

  afterEach(() => httpMock.verify());

  it('should ask the server for the month of a seller without naming any seller', async () => {
    await open('SELLER', 'u-other');

    const request = httpMock.expectOne(isFollowUp);
    expect(request.request.params.has('sellerId')).toBe(false);
    expect(request.request.params.get('month')).toMatch(/^\d{4}-\d{2}$/);
    request.flush(followUp());
    const lines = httpMock.expectOne(isLines);
    expect(lines.request.params.has('sellerId')).toBe(false);
    lines.flush(pageOf([]));
  });

  it('should show the commission of the month and the progress towards the target', async () => {
    await open('SELLER');

    await answer(followUp());

    expect(text()).toContain('850');
    expect(element.querySelector('.ring')?.getAttribute('aria-label')).toContain('85');
    expect(text()).toContain('20000');
  });

  it('should show the base, the rate, the commission and the status of each sale', async () => {
    await open('SELLER');

    await answer(followUp(), [
      commission(),
      commission({ id: 'c2', sourceNumber: 'VTE-0043', status: 'VALIDATED', rate: 4.5 }),
      commission({ id: 'c3', sourceNumber: 'VTE-0044', status: 'CANCELLED' }),
    ]);

    const rows = element.querySelectorAll('tbody tr');
    expect(rows).toHaveLength(3);
    expect(rows[0].textContent).toContain('VTE-0042');
    expect(rows[0].textContent).toContain('5 %');
    expect(rows[0].textContent).toContain('Calculée');
    expect(rows[1].textContent).toContain('4.5 %');
    expect(rows[1].textContent).toContain('Validée');
    expect(rows[2].textContent).toContain('Annulée');
  });

  it('should say that no target is set instead of showing a progress ring', async () => {
    await open('SELLER');

    await answer(followUp({ monthlyTarget: null, targetProgressPercent: null }));

    expect(element.querySelector('.ring')).toBeNull();
    expect(element.textContent).toContain('aucun objectif');
  });

  it('should let the ring stop at 100 % while the text keeps the real progress', async () => {
    await open('SELLER');

    await answer(followUp({ targetProgressPercent: 170, monthlyTarget: 5000 }));

    expect(element.querySelector('.ring-text')?.textContent).toContain('170 %');
    expect(element.querySelector('.arc')?.getAttribute('stroke-dasharray')).toMatch(
      /^(\d+\.\d+) \1$/,
    );
  });

  it('should show an empty state when there is no commission this month', async () => {
    await open('SELLER');

    await answer(followUp({ commissionCount: 0, revenue: 0, commissionAmount: 0 }), []);

    expect(element.textContent).toContain('Aucune commission ce mois-ci');
  });

  it('should ask again when another month is chosen', async () => {
    await open('SELLER');
    await answer(followUp());
    const select = element.querySelector('select') as HTMLSelectElement;
    select.value = select.options[1].value;

    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    const request = httpMock.expectOne(isFollowUp);
    expect(request.request.params.get('month')).toBe(select.options[1].value);
    request.flush(followUp());
    httpMock.expectOne(isLines).flush(pageOf([]));
  });

  it('should ask an administrator to pick a seller and call nothing meanwhile', async () => {
    await open('ADMIN');

    httpMock.expectNone(isFollowUp);
    httpMock.expectNone(isLines);
    expect(element.textContent).toContain('Choisissez une vendeuse');
  });

  it('should send the seller named by the administrator', async () => {
    await open('ADMIN', 'u1');

    const request = httpMock.expectOne(isFollowUp);
    expect(request.request.params.get('sellerId')).toBe('u1');
    request.flush(followUp());
    const lines = httpMock.expectOne(isLines);
    expect(lines.request.params.get('sellerId')).toBe('u1');
    lines.flush(pageOf([]));
  });
});
