import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { SupplierRDto } from '../../data-access/supplier-models';
import { SupplierListPage } from './supplier-list-page';

const supplier = (name: string, type: SupplierRDto['type']): SupplierRDto => ({
  id: `id-${name}`,
  name,
  type,
  contactName: null,
  phone: null,
  email: null,
  address: null,
  notes: null,
  active: true,
  createdAt: '2026-10-21T08:30:00Z',
  updatedAt: '2026-10-21T08:30:00Z',
  version: 0,
});

const huilesRavo = supplier('Huiles Ravo', 'LOCAL');
const provenceEssences = supplier('Provence Essences', 'FOREIGN');

const pageOf = (items: SupplierRDto[], page = 1, totalPages = 1) => ({
  items,
  page,
  pageSize: 20,
  totalCount: items.length,
  totalPages,
});

describe('SupplierListPage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<SupplierListPage>;
  let element: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SupplierListPage],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(SupplierListPage);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  const nextRequest = (): TestRequest =>
    httpMock.expectOne((req) => req.url === '/api/v1/suppliers');

  const respond = async (request: TestRequest, body: object): Promise<void> => {
    request.flush(body);
    await fixture.whenStable();
  };

  const chooseType = async (value: string): Promise<void> => {
    const select = element.querySelector('#supplier-type') as HTMLSelectElement;
    select.value = value;
    select.dispatchEvent(new Event('change'));
    await fixture.whenStable();
  };

  const searchFor = async (term: string): Promise<void> => {
    (element.querySelector('#supplier-search') as HTMLInputElement).value = term;
    (element.querySelector('.toolbar button[type="submit"]') as HTMLButtonElement).click();
    await fixture.whenStable();
  };

  const rowNames = (): string[] =>
    Array.from(element.querySelectorAll('tbody tr td:first-child')).map((cell) =>
      (cell.textContent ?? '').trim(),
    );

  it('should load every type, without filter, sorted by name on opening', async () => {
    const request = nextRequest();

    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('sortBy')).toBe('name');
    expect(request.request.params.has('type')).toBe(false);
    expect(request.request.params.has('search')).toBe(false);
    await respond(request, pageOf([huilesRavo, provenceEssences]));
    expect(rowNames()).toEqual(['Huiles Ravo', 'Provence Essences']);
  });

  it('should reload with the chosen type and show only the returned suppliers', async () => {
    await respond(nextRequest(), pageOf([huilesRavo, provenceEssences]));

    await chooseType('FOREIGN');

    const request = nextRequest();
    expect(request.request.params.get('type')).toBe('FOREIGN');
    await respond(request, pageOf([provenceEssences]));
    expect(rowNames()).toEqual(['Provence Essences']);
  });

  it('should drop the type when "all types" is chosen again', async () => {
    await respond(nextRequest(), pageOf([huilesRavo]));
    await chooseType('LOCAL');
    await respond(nextRequest(), pageOf([huilesRavo]));

    await chooseType('');

    const request = nextRequest();
    expect(request.request.params.has('type')).toBe(false);
    await respond(request, pageOf([huilesRavo, provenceEssences]));
  });

  it('should combine the trimmed search with the chosen type', async () => {
    await respond(nextRequest(), pageOf([huilesRavo]));
    await chooseType('LOCAL');
    await respond(nextRequest(), pageOf([huilesRavo]));

    await searchFor('  Rasoa ');

    const request = nextRequest();
    expect(request.request.params.get('search')).toBe('Rasoa');
    expect(request.request.params.get('type')).toBe('LOCAL');
    await respond(request, pageOf([huilesRavo]));
  });

  it('should go back to the first page when a filter changes', async () => {
    await respond(nextRequest(), pageOf([huilesRavo], 1, 3));
    (element.querySelector('.pagination button:last-of-type') as HTMLButtonElement).click();
    await respond(nextRequest(), pageOf([provenceEssences], 2, 3));

    await chooseType('FOREIGN');

    const request = nextRequest();
    expect(request.request.params.get('page')).toBe('1');
    await respond(request, pageOf([provenceEssences]));
  });

  it('should say that nothing matches when a filter returns no supplier', async () => {
    await respond(nextRequest(), pageOf([huilesRavo]));

    await searchFor('Introuvable');
    await respond(nextRequest(), pageOf([]));

    expect(element.querySelector('.empty')?.textContent).toContain('ne correspond');
  });

  it('should say that there is no supplier yet when the unfiltered list is empty', async () => {
    await respond(nextRequest(), pageOf([]));

    expect(element.querySelector('.empty')?.textContent?.trim()).toBe('Aucun fournisseur');
  });

  it('should ignore the answer of a request overtaken by a newer filter', async () => {
    await respond(nextRequest(), pageOf([huilesRavo, provenceEssences]));
    await chooseType('LOCAL');
    const overtaken = nextRequest();

    await chooseType('FOREIGN');

    expect(overtaken.cancelled).toBe(true);
    await respond(nextRequest(), pageOf([provenceEssences]));
    expect(rowNames()).toEqual(['Provence Essences']);
  });

  it('should show the server message when the list cannot be loaded', async () => {
    nextRequest().flush(
      { title: 'Forbidden', status: 403, detail: 'Accès refusé', code: 'FORBIDDEN' },
      { status: 403, statusText: 'Forbidden' },
    );
    await fixture.whenStable();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Accès refusé');
  });
});
