import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { PageResponse } from '../../../../shared/models/page-response';
import { CommissionRuleRDto } from '../../data-access/commission-rule-models';
import { CommissionRuleListPage } from './commission-rule-list-page';

const RULES_URL = '/api/v1/commission-rules';
const RULE_ID = '0195f0a0-0000-7000-8000-0000000000r1';
const SHOP_ID = '0195f0a0-0000-7000-8000-0000000000c1';

const rule = (overrides: Partial<CommissionRuleRDto> = {}): CommissionRuleRDto => ({
  id: RULE_ID,
  name: 'Boutique standard',
  salesChannelId: SHOP_ID,
  rate: 3.5,
  base: 'MARGIN',
  active: true,
  createdAt: '2026-11-08T08:30:00Z',
  updatedAt: '2026-11-08T08:30:00Z',
  version: 0,
  ...overrides,
});

const pageOf = <T>(items: T[]): PageResponse<T> => ({
  items,
  page: 1,
  pageSize: 10,
  totalCount: items.length,
  totalPages: 1,
});

describe('CommissionRuleListPage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<CommissionRuleListPage>;
  let element: HTMLElement;

  const isListRequest = (req: { url: string }): boolean => req.url === RULES_URL;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommissionRuleListPage],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CommissionRuleListPage);
    element = fixture.nativeElement as HTMLElement;
    httpMock
      .expectOne((req) => req.url === '/api/v1/sales-channels')
      .flush(pageOf([{ id: SHOP_ID, code: 'BOUTIQUE', label: 'Boutique' }]));
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  it('should list name, channel label, rate, base and status of each rule', async () => {
    httpMock
      .expectOne(isListRequest)
      .flush(
        pageOf([
          rule(),
          rule({ id: 'b', name: 'Ancienne', salesChannelId: 'gone', active: false }),
        ]),
      );
    await fixture.whenStable();

    const rows = element.querySelectorAll('tbody tr');
    expect(rows[0].textContent).toContain('Boutique standard');
    expect(rows[0].textContent).toContain('Boutique');
    expect(rows[0].textContent).toContain('3.5 %');
    expect(rows[0].textContent).toContain('Marge brute');
    expect(rows[0].textContent).toContain('ACTIVE');
    expect(rows[1].textContent).toContain('Canal inactif');
    expect(rows[1].textContent).toContain('INACTIVE');
  });

  it('should show the empty state when no rule exists', async () => {
    httpMock.expectOne(isListRequest).flush(pageOf([]));
    await fixture.whenStable();

    expect(element.textContent).toContain('Aucune règle de commission');
  });

  it('should offer no way to delete a rule, only to deactivate it', async () => {
    httpMock.expectOne(isListRequest).flush(pageOf([rule()]));
    await fixture.whenStable();

    const labels = [...element.querySelectorAll('tbody button')].map((button) =>
      button.textContent?.trim(),
    );
    expect(labels).toEqual(['Désactiver']);
  });

  it('should deactivate a rule after confirmation and reload the list', async () => {
    httpMock.expectOne(isListRequest).flush(pageOf([rule()]));
    await fixture.whenStable();

    toggleButton().click();
    await fixture.whenStable();
    dialogButton(/Désactiver/).click();

    const patch = httpMock.expectOne(`${RULES_URL}/${RULE_ID}/status`);
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({ active: false });
    patch.flush(rule({ active: false }));
    httpMock.expectOne(isListRequest).flush(pageOf([rule({ active: false })]));
    await fixture.whenStable();

    expect(element.querySelector('tbody tr')?.textContent).toContain('INACTIVE');
  });

  it('should show the server error when the channel already has another active rule', async () => {
    httpMock.expectOne(isListRequest).flush(pageOf([rule({ active: false })]));
    await fixture.whenStable();

    toggleButton().click();
    await fixture.whenStable();
    dialogButton(/Activer/).click();
    httpMock.expectOne(`${RULES_URL}/${RULE_ID}/status`).flush(
      {
        status: 409,
        code: 'CONFLICT',
        detail: 'Ce canal de vente a déjà une règle de commission active : désactivez-la d’abord.',
      },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'déjà une règle de commission active',
    );
  });

  function dialogButton(label: RegExp): HTMLButtonElement {
    const button = Array.from(element.querySelectorAll<HTMLButtonElement>('dialog button')).find(
      (candidate) => label.test(candidate.textContent ?? ''),
    );
    if (!button) {
      throw new Error('dialog button not found');
    }
    return button;
  }

  function toggleButton(): HTMLButtonElement {
    const button = Array.from(element.querySelectorAll<HTMLButtonElement>('tbody button')).find(
      (candidate) => /Désactiver|Activer/.test(candidate.textContent ?? ''),
    );
    if (!button) {
      throw new Error('toggle button not found');
    }
    return button;
  }
});
