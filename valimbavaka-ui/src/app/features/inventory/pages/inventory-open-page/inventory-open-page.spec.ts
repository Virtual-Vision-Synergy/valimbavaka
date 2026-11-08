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
import { InventoryOpenPage, todayIso } from './inventory-open-page';

const INVENTORIES_URL = '/api/v1/inventories';
const ADMIN_ID = '0195f0a0-0000-7000-8000-000000000001';
const STORE = { id: '0195f0a0-0000-7000-8000-0000000000a2', code: 'STORE', label: 'Boutique' };
const RANGE = { id: '0195f0a0-0000-7000-8000-0000000000b1', code: 'ZEZIKA', label: 'Zezika' };
const page = <T>(items: T[]) => ({
  items,
  page: 1,
  pageSize: 100,
  totalCount: items.length,
  totalPages: 1,
});
const user = (id: string, firstName: string, active: boolean) => ({
  id,
  firstName,
  lastName: 'Rasolofoson',
  active,
});

/** INV-01 : le formulaire d'ouverture exige emplacement, périmètre, responsable et date de comptage. */
describe('InventoryOpenPage', () => {
  const online = signal(true);
  let fixture: ComponentFixture<InventoryOpenPage>;
  let element: HTMLElement;
  let httpMock: HttpTestingController;
  let outbox: OutboxService;
  let toast: ToastService;
  let navigate: MockInstance<Router['navigate']>;

  const open = async (): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [InventoryOpenPage],
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
    fixture = TestBed.createComponent(InventoryOpenPage);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    httpMock.expectOne((req) => req.url === '/api/v1/stock-locations').flush(page([STORE]));
    httpMock.expectOne((req) => req.url === '/api/v1/product-ranges').flush(page([RANGE]));
    httpMock
      .expectOne((req) => req.url === '/api/v1/users')
      .flush(page([user(ADMIN_ID, 'Hanta', true), user('inactive-id', 'Ancien', false)]));
    await fixture.whenStable();
    fixture.detectChanges();
  };

  const select = async (id: string, value: string): Promise<void> => {
    const field = element.querySelector<HTMLSelectElement>(`#${id}`) as HTMLSelectElement;
    field.value = value;
    field.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    fixture.detectChanges();
  };

  const submit = async (): Promise<void> => {
    element.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();
    await fixture.whenStable();
    fixture.detectChanges();
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

  it('should propose the logged-in admin as responsible, today as date and only active accounts', async () => {
    await open();

    expect(element.querySelector<HTMLSelectElement>('#responsibleId')?.value).toBe(ADMIN_ID);
    expect(element.querySelector<HTMLInputElement>('#countDate')?.value).toBe(todayIso());
    expect(element.querySelector('#responsibleId')?.textContent).not.toContain('Ancien');
    expect(element.querySelector('#rangeId')).toBeNull();
  });

  it('should send nothing and flag the missing location and responsible', async () => {
    await open();
    await select('responsibleId', '');

    await submit();

    httpMock.expectNone(INVENTORIES_URL);
    expect(element.querySelector('#locationId-error')?.textContent).toContain('obligatoire');
    expect(element.querySelector('#responsibleId-error')?.textContent).toContain('obligatoire');
    expect(outbox.pendingCount()).toBe(0);
  });

  it('should require a range when the scope is a range', async () => {
    await open();
    await select('locationId', STORE.id);
    await select('scope', 'RANGE');

    await submit();

    httpMock.expectNone(INVENTORIES_URL);
    expect(element.querySelector('#rangeId-error')?.textContent).toContain('gamme');
  });

  it('should refuse a count date in the past', async () => {
    await open();
    await select('locationId', STORE.id);
    const date = element.querySelector<HTMLInputElement>('#countDate') as HTMLInputElement;
    date.value = '2020-01-01';
    date.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    await submit();

    httpMock.expectNone(INVENTORIES_URL);
    expect(element.querySelector('#countDate-error')?.textContent).toContain('passée');
  });

  it('should open the session with the four fields and go back to the inventory', async () => {
    await open();
    await select('locationId', STORE.id);
    await select('scope', 'RANGE');
    await select('rangeId', RANGE.id);

    await submit();

    const request = await vi.waitFor(() => httpMock.expectOne(INVENTORIES_URL));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      locationId: STORE.id,
      scope: 'RANGE',
      rangeId: RANGE.id,
      responsibleId: ADMIN_ID,
      countDate: todayIso(),
    });
    request.flush({ number: 'INV-0012' }, { status: 201, statusText: 'Created' });
    await vi.waitFor(() => expect(navigate).toHaveBeenCalledWith(['/inventory']));
    expect(toast.toasts().at(-1)?.messageKey).toBe('inventory.open.opened');
  });

  it('should keep an opening made offline and say it will be synchronized', async () => {
    await open();
    online.set(false);
    await select('locationId', STORE.id);

    await submit();

    httpMock.expectNone(INVENTORIES_URL);
    expect(outbox.pendingCount()).toBe(1);
    expect(toast.toasts().at(-1)?.messageKey).toBe('common.queuedSaved');
    expect(navigate).toHaveBeenCalledWith(['/inventory']);
  });

  it('should show the conflict when a session is already in progress in the location', async () => {
    await open();
    await select('locationId', STORE.id);
    await submit();

    const request = await vi.waitFor(() => httpMock.expectOne(INVENTORIES_URL));
    request.flush(
      {
        title: 'Conflict',
        status: 409,
        detail: 'Une session d’inventaire est déjà en cours dans cet emplacement.',
        code: 'CONFLICT',
      },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('déjà en cours');
    expect(navigate).not.toHaveBeenCalled();
  });
});
