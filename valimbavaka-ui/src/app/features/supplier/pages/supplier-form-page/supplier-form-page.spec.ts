import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { type MockInstance, vi } from 'vitest';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { SupplierRDto } from '../../data-access/supplier-models';
import { SupplierFormPage } from './supplier-form-page';

const SUPPLIERS_URL = '/api/v1/suppliers';
const ARTICLES_URL = '/api/v1/articles';
const SUPPLIER_URL = '/api/v1/suppliers/0195f0a0-0000-7000-8000-0000000000f1';

const supplier: SupplierRDto = {
  id: '0195f0a0-0000-7000-8000-0000000000f1',
  name: 'Huiles Ravo',
  type: 'LOCAL',
  contactName: 'Rasoa',
  phone: null,
  email: null,
  address: null,
  notes: null,
  active: true,
  createdAt: '2026-10-21T08:30:00Z',
  updatedAt: '2026-10-22T08:30:00Z',
  version: 3,
};

const conflict = (detail: string) => ({ title: 'Conflict', status: 409, detail, code: 'CONFLICT' });

describe('SupplierFormPage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<SupplierFormPage>;
  let element: HTMLElement;
  let navigate: MockInstance<Router['navigate']>;

  const setup = async (id?: string): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [SupplierFormPage],
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
    fixture = TestBed.createComponent(SupplierFormPage);
    if (id) {
      fixture.componentRef.setInput('id', id);
    }
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  };

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  const flushArticles = (items: unknown[] = []): void => {
    httpMock
      .expectOne((req) => req.url === ARTICLES_URL)
      .flush({ items, page: 1, pageSize: 100, totalCount: items.length, totalPages: 1 });
  };

  const field = <T extends HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>(
    id: string,
  ): T => element.querySelector<T>(`#${id}`) as T;

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

  describe('in edit mode', () => {
    beforeEach(async () => {
      await setup(supplier.id);
      httpMock.expectOne(SUPPLIER_URL).flush(supplier);
      flushArticles();
      await fixture.whenStable();
    });

    it('should say that no article is supplied when the list is empty', () => {
      expect(element.querySelector('.articles')?.textContent).toContain(
        'Aucun article fourni par ce fournisseur.',
      );
    });

    it('should fill the form with the sheet read from the server', () => {
      expect(field<HTMLInputElement>('name').value).toBe('Huiles Ravo');
      expect(field<HTMLSelectElement>('type').value).toBe('LOCAL');
      expect(field<HTMLInputElement>('contactName').value).toBe('Rasoa');
      expect(field<HTMLInputElement>('phone').value).toBe('');
    });

    it('should save every edited field with PUT then go back to the list', async () => {
      await type('name', 'Import Océan');
      await type('type', 'FOREIGN');
      await type('contactName', 'Mr Lee');
      await type('phone', '+33 1 00 00 00 00');
      await type('email', 'lee@ocean.com');
      await type('address', 'Marseille');
      await type('notes', 'Livraison le mardi');

      await save();

      const request = httpMock.expectOne(SUPPLIER_URL);
      expect(request.request.method).toBe('PUT');
      expect(request.request.body).toEqual({
        name: 'Import Océan',
        type: 'FOREIGN',
        contactName: 'Mr Lee',
        phone: '+33 1 00 00 00 00',
        email: 'lee@ocean.com',
        address: 'Marseille',
        notes: 'Livraison le mardi',
        version: 3,
      });
      request.flush({ ...supplier, name: 'Import Océan', version: 4 });
      await fixture.whenStable();
      expect(navigate).toHaveBeenCalledWith(['/suppliers']);
    });

    it('should send back the version read so that a concurrent change is refused', async () => {
      await save();

      const request = httpMock.expectOne(SUPPLIER_URL);
      expect(request.request.method).toBe('PUT');
      expect(request.request.body).toMatchObject({ name: 'Huiles Ravo', version: 3 });
      request.flush({ ...supplier, version: 4 });
    });

    it('should show the server message when the sheet was modified in the meantime', async () => {
      await save();

      httpMock
        .expectOne(SUPPLIER_URL)
        .flush(
          conflict('Le fournisseur a été modifié entre-temps : rechargez la fiche puis réessayez.'),
          {
            status: 409,
            statusText: 'Conflict',
          },
        );
      await fixture.whenStable();

      expect(element.textContent).toContain('modifié entre-temps');
      expect(navigate).not.toHaveBeenCalled();
      expect(field<HTMLInputElement>('name').value).toBe('Huiles Ravo');
    });

    it('should send nothing and ask for a name when the name is only spaces', async () => {
      await type('name', '   ');

      await save();

      httpMock.expectNone(SUPPLIER_URL);
      expect(element.querySelector('#name-error')?.textContent).toContain('Le nom est obligatoire');
    });

    it('should send nothing and show the length message when the name exceeds 150 characters', async () => {
      await type('name', 'a'.repeat(151));

      await save();

      httpMock.expectNone(SUPPLIER_URL);
      expect(element.querySelector('#name-error')?.textContent).toContain('150 caractères');
    });

    it('should accept a name of exactly 150 characters', async () => {
      await type('name', 'a'.repeat(150));

      await save();

      expect(httpMock.expectOne(SUPPLIER_URL).request.body).toMatchObject({
        name: 'a'.repeat(150),
      });
    });

    it.each([
      ['contactName', 150],
      ['phone', 50],
      ['email', 255],
      ['address', 500],
      ['notes', 2000],
    ])(
      'should send nothing and explain the limit when %s exceeds %i characters',
      async (id, max) => {
        await type(id, 'a'.repeat(max + 1));

        await save();

        httpMock.expectNone(SUPPLIER_URL);
        expect(element.querySelector(`#${id}-error`)?.textContent).toContain(
          `${max} caractères maximum`,
        );
      },
    );

    it('should accept every field at its maximum length (email 254, the Angular cap)', async () => {
      const values = {
        contactName: 'c'.repeat(150),
        phone: '0'.repeat(50),
        email: `${'e'.repeat(64)}@${'d'.repeat(63)}.${'d'.repeat(63)}.${'d'.repeat(58)}.mg`,
        address: 'a'.repeat(500),
        notes: 'n'.repeat(2000),
      };
      for (const [id, value] of Object.entries(values)) {
        await type(id, value);
      }

      await save();

      // 254 : plafond d'Angular (RFC 5321) ; le serveur accepte 255.
      expect(values.email).toHaveLength(254);
      expect(httpMock.expectOne(SUPPLIER_URL).request.body).toMatchObject(values);
    });

    it('should send nothing when the email is invalid', async () => {
      await type('email', 'abc');

      await save();

      httpMock.expectNone(SUPPLIER_URL);
      expect(element.querySelector('#email-error')?.textContent).toContain(
        "L'email n'est pas valide",
      );
    });

    it('should disable the save button while saving so that a double click sends one request', async () => {
      await save();
      await save();

      const button = element.querySelector<HTMLButtonElement>('button[type="submit"]');
      expect(button?.disabled).toBe(true);
      httpMock.expectOne(SUPPLIER_URL).flush(supplier);
      await fixture.whenStable();
      expect(button?.disabled).toBe(false);
    });
  });

  describe('in creation mode', () => {
    beforeEach(async () => {
      await setup();
      await fixture.whenStable();
    });

    it('should create with POST, without version, then go back to the list', async () => {
      await type('name', '  Coopérative Itasy ');
      await type('type', 'FOREIGN');

      await save();

      const request = httpMock.expectOne(SUPPLIERS_URL);
      expect(request.request.method).toBe('POST');
      expect(request.request.body).toMatchObject({
        name: '  Coopérative Itasy ',
        type: 'FOREIGN',
        contactName: '',
        version: null,
      });
      request.flush(
        { ...supplier, name: 'Coopérative Itasy' },
        { status: 201, statusText: 'Created' },
      );
      await fixture.whenStable();
      expect(navigate).toHaveBeenCalledWith(['/suppliers']);
    });

    it('should show the server message when the name is already used', async () => {
      await type('name', 'HUILES RAVO');

      await save();

      httpMock.expectOne(SUPPLIERS_URL).flush(conflict('Un fournisseur avec ce nom existe déjà.'), {
        status: 409,
        statusText: 'Conflict',
      });
      await fixture.whenStable();

      expect(element.querySelector('[role="alert"]')?.textContent).toContain('existe déjà');
      expect(navigate).not.toHaveBeenCalled();
    });

    it('should send nothing when the name is empty', async () => {
      await save();

      httpMock.expectNone(SUPPLIERS_URL);
      expect(element.querySelector('#name-error')).not.toBeNull();
    });
  });

  describe('with supplied articles', () => {
    it('should list the articles of this supplier, read with its identifier', async () => {
      await setup(supplier.id);
      httpMock.expectOne(SUPPLIER_URL).flush(supplier);
      const request = httpMock.expectOne((req) => req.url === ARTICLES_URL);
      expect(request.request.params.get('supplierId')).toBe(supplier.id);
      request.flush({
        items: [{ id: 'a1', code: 'ART-0031', name: 'Beurre de karité brut' }],
        page: 1,
        pageSize: 100,
        totalCount: 1,
        totalPages: 1,
      });
      await fixture.whenStable();

      fixture.detectChanges();
      expect(element.querySelector('.supplied')?.textContent).toContain('ART-0031');
      expect(element.querySelector('.supplied')?.textContent).toContain('Beurre de karité brut');
    });
  });

  describe('when the sheet does not exist', () => {
    it('should show the server message', async () => {
      await setup(supplier.id);

      httpMock.expectOne(SUPPLIER_URL).flush(
        {
          title: 'Not Found',
          status: 404,
          detail: `Fournisseur introuvable : ${supplier.id}`,
          code: 'NOT_FOUND',
        },
        { status: 404, statusText: 'Not Found' },
      );
      flushArticles();
      await fixture.whenStable();

      expect(element.querySelector('[role="alert"]')?.textContent).toContain(
        'Fournisseur introuvable',
      );
    });
  });
});
