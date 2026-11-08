import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { type MockInstance, vi } from 'vitest';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ConnectivityService } from '../../../../core/offline/connectivity-service';
import { OutboxService } from '../../../../core/offline/outbox-service';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { CommissionRuleRDto } from '../../data-access/commission-rule-models';
import { CommissionRuleFormPage } from './commission-rule-form-page';

const RULES_URL = '/api/v1/commission-rules';
const CHANNELS_URL = '/api/v1/sales-channels';
const FACEBOOK_ID = '0195f0a0-0000-7000-8000-0000000000c2';

const saved: CommissionRuleRDto = {
  id: '0195f0a0-0000-7000-8000-0000000000r2',
  name: 'Facebook live',
  salesChannelId: FACEBOOK_ID,
  rate: 5.25,
  base: 'MARGIN',
  active: true,
  createdAt: '2026-11-08T08:30:00Z',
  updatedAt: '2026-11-08T08:30:00Z',
  version: 0,
};

/**
 * COM-06 : une règle saisie dans le formulaire n'entre dans le calcul qu'une fois enregistrée par le serveur.
 * Hors connexion, elle reste dans la file (SYS-07) et part une seule fois, avec la même clé d'idempotence.
 */
describe('CommissionRuleFormPage with the offline queue', () => {
  const online = signal(true);
  let fixture: ComponentFixture<CommissionRuleFormPage>;
  let element: HTMLElement;
  let httpMock: HttpTestingController;
  let outbox: OutboxService;
  let toast: ToastService;
  let navigate: MockInstance<Router['navigate']>;

  const open = async (): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [CommissionRuleFormPage],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
        { provide: ConnectivityService, useValue: { isOnline: online.asReadonly() } },
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
    httpMock = TestBed.inject(HttpTestingController);
    outbox = TestBed.inject(OutboxService);
    toast = TestBed.inject(ToastService);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(CommissionRuleFormPage);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    httpMock
      .expectOne((req) => req.url === CHANNELS_URL)
      .flush({
        items: [{ id: FACEBOOK_ID, code: 'FACEBOOK', label: 'Facebook' }],
        page: 1,
        pageSize: 100,
        totalCount: 1,
        totalPages: 1,
      });
    await fixture.whenStable();
  };

  const type = async (id: string, value: string): Promise<void> => {
    const control = element.querySelector<HTMLInputElement | HTMLSelectElement>(`#${id}`);
    if (!control) {
      throw new Error(`champ #${id} introuvable`);
    }
    control.value = value;
    control.dispatchEvent(new Event(control instanceof HTMLSelectElement ? 'change' : 'input'));
    await fixture.whenStable();
  };

  const fillFacebookLive = async (): Promise<void> => {
    await type('name', 'Facebook live');
    await type('salesChannelId', FACEBOOK_ID);
    await type('rate', '5.25');
    await type('base', 'MARGIN');
  };

  const save = async (): Promise<void> => {
    element.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();
    await fixture.whenStable();
  };

  beforeEach(() => online.set(true));

  afterEach(() => {
    try {
      httpMock.verify();
    } finally {
      vi.restoreAllMocks();
      TestBed.resetTestingModule();
    }
  });

  it('should send the rule with an idempotency key when online and show no waiting message', async () => {
    await open();
    await fillFacebookLive();

    await save();

    const request = httpMock.expectOne(RULES_URL);
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
    request.flush(saved, { status: 201, statusText: 'Created' });
    await fixture.whenStable();
    expect(navigate).toHaveBeenCalledWith(['/commissions/rules']);
    expect(toast.toasts()).toHaveLength(0);
  });

  it('should keep a rule entered offline in the queue, say so and go back to the rules', async () => {
    await open();
    online.set(false);
    await fillFacebookLive();

    await save();

    httpMock.expectNone(RULES_URL);
    expect(outbox.pendingCount()).toBe(1);
    expect(toast.toasts().at(-1)?.messageKey).toBe('common.queuedSaved');
    expect(navigate).toHaveBeenCalledWith(['/commissions/rules']);
  });

  it('should send the queued rule once with its idempotency key when the network is back', async () => {
    await open();
    online.set(false);
    await fillFacebookLive();
    await save();
    TestBed.tick();

    online.set(true);
    TestBed.tick();

    const request = await vi.waitFor(() => httpMock.expectOne(RULES_URL));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toMatchObject({
      name: 'Facebook live',
      salesChannelId: FACEBOOK_ID,
      rate: 5.25,
      base: 'MARGIN',
    });
    expect(request.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
    request.flush(saved, { status: 201, statusText: 'Created' });
    await vi.waitFor(() => expect(outbox.pendingCount()).toBe(0));
  });

  it('should queue nothing when the form is invalid, even offline', async () => {
    await open();
    online.set(false);
    await type('name', '   ');

    await save();

    expect(outbox.pendingCount()).toBe(0);
    expect(navigate).not.toHaveBeenCalled();
    expect(toast.toasts()).toHaveLength(0);
  });
});
