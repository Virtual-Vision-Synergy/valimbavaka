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
import { SalonServiceRDto } from '../../data-access/salon-catalog-models';
import { SalonCatalogListPage } from './salon-catalog-list-page';

const SERVICES_URL = '/api/v1/salon-services';
const SERVICE_ID = '0195f0a0-0000-7000-8000-0000000000s1';

const service = (overrides: Partial<SalonServiceRDto> = {}): SalonServiceRDto => ({
  id: SERVICE_ID,
  name: 'Soin visage éclat',
  durationMinutes: 45,
  price: 35000,
  commissionRate: 12.5,
  products: [
    { articleId: 'a1', quantity: 0.015 },
    { articleId: 'a2', quantity: 1 },
  ],
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

describe('SalonCatalogListPage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<SalonCatalogListPage>;
  let element: HTMLElement;

  const isListRequest = (req: { url: string }): boolean => req.url === SERVICES_URL;

  const setup = async (role: 'ADMIN' | 'SELLER'): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [SalonCatalogListPage],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser(role));
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(SalonCatalogListPage);
    element = fixture.nativeElement as HTMLElement;
  };

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  it('should list name, duration, price, commission and number of products', async () => {
    await setup('ADMIN');
    httpMock
      .expectOne(isListRequest)
      .flush(
        pageOf([
          service(),
          service({ id: 'b', name: 'Épilation sourcils', commissionRate: null, products: [] }),
        ]),
      );
    await fixture.whenStable();

    const rows = element.querySelectorAll('tbody tr');
    expect(rows[0].textContent).toContain('Soin visage éclat');
    expect(rows[0].textContent).toContain('45 min');
    expect(rows[0].textContent).toContain('Ar');
    expect(rows[0].textContent).toContain('12.5 %');
    expect(rows[0].textContent).toContain('2 produit(s)');
    expect(rows[1].textContent).toContain('Canal ou vendeuse');
    expect(rows[1].textContent).toContain('0 produit(s)');
  });

  it('should show the empty state when the catalogue is empty', async () => {
    await setup('ADMIN');
    httpMock.expectOne(isListRequest).flush(pageOf([]));
    await fixture.whenStable();

    expect(element.textContent).toContain('Aucun service salon');
  });

  it('should let the seller read the catalogue without any write action', async () => {
    await setup('SELLER');
    httpMock.expectOne(isListRequest).flush(pageOf([service()]));
    await fixture.whenStable();

    expect(element.querySelector('tbody tr')?.textContent).toContain('Soin visage éclat');
    expect(element.textContent).not.toContain('Nouveau service');
    expect(element.querySelectorAll('tbody button, tbody a')).toHaveLength(0);
  });

  it('should deactivate a service after confirmation and reload the list', async () => {
    await setup('ADMIN');
    httpMock.expectOne(isListRequest).flush(pageOf([service()]));
    await fixture.whenStable();

    const labels = [...element.querySelectorAll('tbody button')].map((button) =>
      button.textContent?.trim(),
    );
    expect(labels).toEqual(['Désactiver']);
    element.querySelector<HTMLButtonElement>('tbody button')?.click();
    await fixture.whenStable();
    Array.from(element.querySelectorAll<HTMLButtonElement>('dialog button'))
      .find((button) => /Désactiver/.test(button.textContent ?? ''))
      ?.click();

    const patch = httpMock.expectOne(`${SERVICES_URL}/${SERVICE_ID}/status`);
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({ active: false });
    patch.flush(service({ active: false }));
    httpMock.expectOne(isListRequest).flush(pageOf([service({ active: false })]));
    await fixture.whenStable();

    expect(element.querySelector('tbody tr')?.textContent).toContain('INACTIF');
  });
});
