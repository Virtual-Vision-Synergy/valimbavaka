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
import { SupplierFormPage } from './supplier-form-page';

const SUPPLIERS_URL = '/api/v1/suppliers';
const SUPPLIER_ID = '0195f0a0-0000-7000-8000-0000000000f1';
const SUPPLIER_URL = `${SUPPLIERS_URL}/${SUPPLIER_ID}`;
const saved = { id: SUPPLIER_ID, name: 'Huiles Ravo', type: 'LOCAL', active: true, version: 0 };

/**
 * Formulaire de référence de la file hors ligne (SYS-07) : la saisie fournisseur passe par `OutboxService`,
 * elle est conservée hors connexion puis envoyée une seule fois, avec la même clé d'idempotence.
 */
describe('SupplierFormPage with the offline queue', () => {
  const online = signal(true);
  let fixture: ComponentFixture<SupplierFormPage>;
  let element: HTMLElement;
  let httpMock: HttpTestingController;
  let outbox: OutboxService;
  let toast: ToastService;
  let navigate: MockInstance<Router['navigate']>;

  const open = async (id?: string): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [SupplierFormPage],
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
    fixture = TestBed.createComponent(SupplierFormPage);
    if (id) {
      fixture.componentRef.setInput('id', id);
    }
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  };

  const readSheet = async (): Promise<void> => {
    httpMock.expectOne(SUPPLIER_URL).flush({ ...saved, version: 3 });
    httpMock
      .expectOne((req) => req.url === '/api/v1/articles')
      .flush({ items: [], page: 1, pageSize: 100, totalCount: 0, totalPages: 1 });
    await fixture.whenStable();
    fixture.detectChanges();
  };

  const typeName = async (name: string): Promise<void> => {
    const input = element.querySelector<HTMLInputElement>('#name') as HTMLInputElement;
    input.value = name;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
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

  it('should send a creation with an idempotency key when online and go back to the list', async () => {
    await open();
    await typeName('Huiles Ravo');

    await save();

    const request = httpMock.expectOne(SUPPLIERS_URL);
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
    request.flush(saved, { status: 201, statusText: 'Created' });
    await fixture.whenStable();
    expect(navigate).toHaveBeenCalledWith(['/suppliers']);
    expect(toast.toasts()).toHaveLength(0);
  });

  it('should keep a creation made offline, say it will be synchronized and go back to the list', async () => {
    await open();
    online.set(false);
    await typeName('Huiles Ravo');

    await save();

    httpMock.expectNone(SUPPLIERS_URL);
    expect(outbox.pendingCount()).toBe(1);
    expect(toast.toasts().at(-1)?.messageKey).toBe('common.queuedSaved');
    expect(navigate).toHaveBeenCalledWith(['/suppliers']);
  });

  it('should send the queued creation once, with the same key, when the network is back', async () => {
    await open();
    online.set(false);
    await typeName('Huiles Ravo');
    await save();
    TestBed.tick();

    online.set(true);
    TestBed.tick();

    const request = await vi.waitFor(() => httpMock.expectOne(SUPPLIERS_URL));
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toMatchObject({ name: 'Huiles Ravo', type: 'LOCAL' });
    const key = request.request.headers.get('Idempotency-Key');
    expect(key).toMatch(/^[0-9a-f-]{36}$/);
    request.flush(saved, { status: 201, statusText: 'Created' });
    await vi.waitFor(() => expect(outbox.pendingCount()).toBe(0));
  });

  it('should queue nothing when the form is invalid, even offline', async () => {
    await open();
    online.set(false);
    await typeName('   ');

    await save();

    expect(outbox.pendingCount()).toBe(0);
    expect(navigate).not.toHaveBeenCalled();
    expect(toast.toasts()).toHaveLength(0);
  });

  it('should queue a modification made offline with the version read', async () => {
    await open(SUPPLIER_ID);
    await readSheet();
    online.set(false);
    await typeName('Huiles Ravo Plus');

    await save();

    expect(outbox.pendingCount()).toBe(1);
    expect(toast.toasts().at(-1)?.messageKey).toBe('common.queuedSaved');
    online.set(true);
    TestBed.tick();
    const request = await vi.waitFor(() => httpMock.expectOne(SUPPLIER_URL));
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toMatchObject({ name: 'Huiles Ravo Plus', version: 3 });
    request.flush({ ...saved, name: 'Huiles Ravo Plus', version: 4 });
    await vi.waitFor(() => expect(outbox.pendingCount()).toBe(0));
  });

  it('should keep the server refusal on screen when online, without queueing it', async () => {
    await open();
    await typeName('Huiles Ravo');
    await save();

    httpMock.expectOne(SUPPLIERS_URL).flush(
      {
        title: 'Conflict',
        status: 409,
        detail: 'Un fournisseur porte déjà ce nom.',
        code: 'CONFLICT',
      },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('porte déjà ce nom');
    expect(outbox.pendingCount()).toBe(0);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('should keep the save button disabled until the sheet is read', async () => {
    await open(SUPPLIER_ID);
    const button = (): HTMLButtonElement =>
      element.querySelector<HTMLButtonElement>('button[type="submit"]') as HTMLButtonElement;
    expect(button().disabled).toBe(true);

    await readSheet();

    expect(button().disabled).toBe(false);
  });

  it('should send nothing when the sheet could not be read', async () => {
    await open(SUPPLIER_ID);
    httpMock
      .expectOne(SUPPLIER_URL)
      .flush(
        { title: 'Erreur', status: 500, detail: 'Erreur interne du serveur.', code: 'INTERNAL' },
        { status: 500, statusText: 'Server Error' },
      );
    httpMock
      .expectOne((req) => req.url === '/api/v1/articles')
      .flush({ items: [], page: 1, pageSize: 100, totalCount: 0, totalPages: 1 });
    await fixture.whenStable();
    fixture.detectChanges();
    await typeName('Nom saisi sans fiche lue');

    await save();

    httpMock.expectNone(SUPPLIER_URL);
    expect(outbox.pendingCount()).toBe(0);
    expect(element.querySelector('[role="alert"]')).not.toBeNull();
  });
});
