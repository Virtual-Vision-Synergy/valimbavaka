import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { type MockInstance, vi } from 'vitest';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { CommissionRuleRDto } from '../../data-access/commission-rule-models';
import { CommissionRuleFormPage } from './commission-rule-form-page';

const RULES_URL = '/api/v1/commission-rules';
const CHANNELS_URL = '/api/v1/sales-channels';
const SHOP_ID = '0195f0a0-0000-7000-8000-0000000000c1';
const FACEBOOK_ID = '0195f0a0-0000-7000-8000-0000000000c2';

const channels = [
  { id: SHOP_ID, code: 'BOUTIQUE', label: 'Boutique' },
  { id: FACEBOOK_ID, code: 'FACEBOOK', label: 'Facebook' },
];

const rule: CommissionRuleRDto = {
  id: '0195f0a0-0000-7000-8000-0000000000r1',
  name: 'Boutique standard',
  salesChannelId: SHOP_ID,
  rate: 3.5,
  base: 'REVENUE',
  active: true,
  createdAt: '2026-11-08T08:30:00Z',
  updatedAt: '2026-11-08T08:30:00Z',
  version: 2,
};

const problem = (status: number, code: string, detail: string) => ({
  title: code,
  status,
  detail,
  code,
});

describe('CommissionRuleFormPage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<CommissionRuleFormPage>;
  let element: HTMLElement;
  let navigate: MockInstance<Router['navigate']>;

  const setup = async (id?: string): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [CommissionRuleFormPage],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN')); // les écritures passent par la file hors ligne
    httpMock = TestBed.inject(HttpTestingController);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(CommissionRuleFormPage);
    if (id) {
      fixture.componentRef.setInput('id', id);
    }
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    httpMock
      .expectOne((req) => req.url === CHANNELS_URL)
      .flush({ items: channels, page: 1, pageSize: 100, totalCount: 2, totalPages: 1 });
  };

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  const field = <T extends HTMLInputElement | HTMLSelectElement>(id: string): T =>
    element.querySelector<T>(`#${id}`) as T;

  const type = async (id: string, value: string): Promise<void> => {
    const control = field(id);
    control.value = value;
    control.dispatchEvent(new Event(control instanceof HTMLSelectElement ? 'change' : 'input'));
    await fixture.whenStable();
  };

  const save = async (): Promise<void> => {
    element.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();
    await fixture.whenStable();
  };

  describe('in creation mode', () => {
    beforeEach(async () => {
      await setup();
      await fixture.whenStable();
    });

    it('should offer name, sales channel, rate and calculation base', () => {
      const labels = Array.from(element.querySelectorAll('label')).map((label) =>
        label.textContent?.trim(),
      );

      expect(labels).toEqual(['Nom *', 'Canal de vente *', 'Taux (%) *', 'Base de calcul *']);
      const channelOptions = Array.from(field<HTMLSelectElement>('salesChannelId').options);
      expect(channelOptions.map((option) => option.textContent?.trim())).toContain('Facebook');
      const baseOptions = Array.from(field<HTMLSelectElement>('base').options);
      expect(baseOptions.map((option) => option.textContent?.trim())).toEqual([
        "Chiffre d'affaires",
        'Marge brute',
      ]);
    });

    it('should create the rule with POST then go back to the rules', async () => {
      await type('name', 'Facebook live');
      await type('salesChannelId', FACEBOOK_ID);
      await type('rate', '5.25');
      await type('base', 'MARGIN');

      await save();

      const request = httpMock.expectOne(RULES_URL);
      expect(request.request.method).toBe('POST');
      expect(request.request.body).toEqual({
        name: 'Facebook live',
        salesChannelId: FACEBOOK_ID,
        rate: 5.25,
        base: 'MARGIN',
        version: null,
      });
      request.flush({ ...rule, name: 'Facebook live' });
      await fixture.whenStable();
      expect(navigate).toHaveBeenCalledWith(['/commissions/rules']);
    });

    it.each([
      ['100.01', 'au-dessus de 100'],
      ['-1', 'négatif'],
      ['3.555', 'trois décimales'],
    ])('should send nothing and explain the bounds when the rate is %s (%s)', async (rate) => {
      await type('name', 'Boutique standard');
      await type('salesChannelId', SHOP_ID);
      await type('rate', rate);

      await save();

      httpMock.expectNone(RULES_URL);
      expect(element.querySelector('#rate-error')?.textContent).toContain('entre 0 et 100');
    });

    it('should accept a rate of exactly 100', async () => {
      await type('name', 'Boutique standard');
      await type('salesChannelId', SHOP_ID);
      await type('rate', '100');

      await save();

      expect(httpMock.expectOne(RULES_URL).request.body).toMatchObject({ rate: 100 });
    });

    it('should ask for a name and a channel when they are missing', async () => {
      await type('name', '   ');
      await type('rate', '3');

      await save();

      httpMock.expectNone(RULES_URL);
      expect(element.querySelector('#name-error')?.textContent).toContain('Le nom est obligatoire');
      expect(element.querySelector('#salesChannelId-error')?.textContent).toContain(
        'Le canal de vente est obligatoire',
      );
    });

    it('should show the server message when the channel already has an active rule', async () => {
      await type('name', 'Boutique promo');
      await type('salesChannelId', SHOP_ID);
      await type('rate', '4');

      await save();

      httpMock
        .expectOne(RULES_URL)
        .flush(
          problem(409, 'CONFLICT', 'Ce canal de vente a déjà une règle de commission active.'),
          { status: 409, statusText: 'Conflict' },
        );
      await fixture.whenStable();

      expect(element.querySelector('[role="alert"]')?.textContent).toContain(
        'déjà une règle de commission active',
      );
      expect(navigate).not.toHaveBeenCalled();
    });
  });

  describe('in edit mode', () => {
    beforeEach(async () => {
      await setup(rule.id);
      httpMock.expectOne(`${RULES_URL}/${rule.id}`).flush(rule);
      await fixture.whenStable();
    });

    it('should fill the form with the rule read from the server', () => {
      expect(field<HTMLInputElement>('name').value).toBe('Boutique standard');
      expect(field<HTMLSelectElement>('salesChannelId').value).toBe(SHOP_ID);
      expect(field<HTMLInputElement>('rate').value).toBe('3.5');
      expect(field<HTMLSelectElement>('base').value).toBe('REVENUE');
    });

    it('should save with PUT and send back the version read', async () => {
      await type('rate', '4');

      await save();

      const request = httpMock.expectOne(`${RULES_URL}/${rule.id}`);
      expect(request.request.method).toBe('PUT');
      expect(request.request.body).toEqual({
        name: 'Boutique standard',
        salesChannelId: SHOP_ID,
        rate: 4,
        base: 'REVENUE',
        version: 2,
      });
      request.flush({ ...rule, rate: 4, version: 3 });
    });
  });
});
