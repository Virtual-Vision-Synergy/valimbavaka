import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { SupplierRDto } from '../../data-access/supplier-models';
import { SupplierFormPage } from './supplier-form-page';

const SUPPLIER_ID = '0195f0a0-0000-7000-8000-0000000000f1';
const SUPPLIER_URL = `/api/v1/suppliers/${SUPPLIER_ID}`;

const supplier: SupplierRDto = {
  id: SUPPLIER_ID,
  name: 'Huiles Ravo',
  type: 'LOCAL',
  contactName: null,
  phone: null,
  email: null,
  address: null,
  notes: null,
  leadTimeDays: 7,
  active: true,
  createdAt: '2026-10-21T08:30:00Z',
  updatedAt: '2026-10-22T08:30:00Z',
  version: 3,
};

/** FRS-04 : délai de réapprovisionnement proposé selon le type, modifiable, borné de 0 à 365 jours. */
describe('SupplierFormPage — délai de réapprovisionnement', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<SupplierFormPage>;
  let element: HTMLElement;

  const setup = async (sheet?: SupplierRDto): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [SupplierFormPage],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
    httpMock = TestBed.inject(HttpTestingController);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(SupplierFormPage);
    if (sheet) {
      fixture.componentRef.setInput('id', sheet.id);
    }
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    if (sheet) {
      httpMock.expectOne(SUPPLIER_URL).flush(sheet);
      httpMock
        .expectOne((req) => req.url === '/api/v1/articles')
        .flush({ items: [], page: 1, pageSize: 100, totalCount: 0, totalPages: 1 });
    }
    await fixture.whenStable();
  };

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  const leadTime = (): string =>
    element.querySelector<HTMLInputElement>('#leadTimeDays')?.value ?? '';

  const type = async (id: string, value: string): Promise<void> => {
    const control = element.querySelector<HTMLInputElement | HTMLSelectElement>(`#${id}`);
    if (!control) {
      throw new Error(`Champ #${id} absent`);
    }
    control.value = value;
    control.dispatchEvent(new Event(control instanceof HTMLSelectElement ? 'change' : 'input'));
    await fixture.whenStable();
  };

  const save = async (): Promise<void> => {
    element.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();
    await fixture.whenStable();
  };

  describe('en création', () => {
    beforeEach(() => setup());

    it('should propose 7 days for a local supplier then 30 days once foreign', async () => {
      expect(leadTime()).toBe('7');
      expect(element.querySelector('#leadTimeDays-hint')?.textContent).toContain(
        '7 jours pour un fournisseur local, 30 jours pour un fournisseur étranger',
      );

      await type('type', 'FOREIGN');

      expect(leadTime()).toBe('30');
    });
  });

  describe('en modification', () => {
    it('should keep a lead time typed by hand when the type changes', async () => {
      await setup(supplier);
      await type('leadTimeDays', '12');
      await type('type', 'FOREIGN');

      await save();

      expect(httpMock.expectOne(SUPPLIER_URL).request.body).toMatchObject({
        type: 'FOREIGN',
        leadTimeDays: 12,
      });
    });

    it('should show the lead time read and keep it when the type changes', async () => {
      await setup({ ...supplier, type: 'FOREIGN', leadTimeDays: 45 });

      expect(leadTime()).toBe('45');
      await type('type', 'LOCAL');
      expect(leadTime()).toBe('45');
    });

    it('should switch a lead time left at the default of the type read to the new default', async () => {
      await setup({ ...supplier, type: 'FOREIGN', leadTimeDays: 30 });

      await type('type', 'LOCAL');

      expect(leadTime()).toBe('7');
    });

    it.each(['0', '365'])('should accept a lead time of %s days (bounds)', async (value) => {
      await setup(supplier);
      await type('leadTimeDays', value);

      await save();

      expect(httpMock.expectOne(SUPPLIER_URL).request.body).toMatchObject({
        leadTimeDays: Number(value),
      });
    });

    it.each(['-1', '366', '2.5', ''])(
      'should send nothing and explain the bounds when the lead time is "%s"',
      async (value) => {
        await setup(supplier);
        await type('leadTimeDays', value);

        await save();

        httpMock.expectNone(SUPPLIER_URL);
        expect(element.querySelector('#leadTimeDays-error')?.textContent).toContain(
          'Nombre entier de jours entre 0 et 365',
        );
      },
    );
  });
});
