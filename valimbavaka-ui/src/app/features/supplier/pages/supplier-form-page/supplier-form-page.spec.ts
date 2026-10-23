import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { SupplierRDto } from '../../data-access/supplier-models';
import { SupplierFormPage } from './supplier-form-page';

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

describe('SupplierFormPage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<SupplierFormPage>;
  let element: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SupplierFormPage],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(SupplierFormPage);
    fixture.componentRef.setInput('id', supplier.id);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    httpMock.expectOne(SUPPLIER_URL).flush(supplier);
    await fixture.whenStable();
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  const save = async (): Promise<void> => {
    element.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();
    await fixture.whenStable();
  };

  it('should send back the version read so that a concurrent change is refused', async () => {
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    await save();

    const request = httpMock.expectOne(SUPPLIER_URL);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toMatchObject({ name: 'Huiles Ravo', version: 3 });
    request.flush({ ...supplier, version: 4 });
  });

  it('should show the server message when the sheet was modified in the meantime', async () => {
    await save();

    httpMock.expectOne(SUPPLIER_URL).flush(
      {
        title: 'Conflict',
        status: 409,
        detail: 'The supplier was modified in the meantime: reload it and try again.',
        code: 'CONFLICT',
      },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();

    expect(element.textContent).toContain('modified in the meantime');
  });
});
