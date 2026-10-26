import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { SupplierListPage } from './supplier-list-page';
import { SUPPLIERS_URL, huilesRavo, pageOf, provenceEssences } from './supplier-list-page.fixtures';

/** Liste des fournisseurs de la maquette, branchée sur `/api/v1/suppliers` (FRS-01, FRS-03). */
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
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(SupplierListPage);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    vi.useRealTimers();
  });

  const nextRequest = (): TestRequest => httpMock.expectOne((req) => req.url === SUPPLIERS_URL);

  const respond = async (request: TestRequest, body: object): Promise<void> => {
    request.flush(body);
    await fixture.whenStable();
    fixture.detectChanges();
  };

  const chooseType = async (label: string): Promise<void> => {
    Array.from(element.querySelectorAll<HTMLButtonElement>('.chips button'))
      .find((chip) => chip.textContent?.includes(label))
      ?.click();
    await fixture.whenStable();
    fixture.detectChanges();
  };

  const searchFor = async (term: string): Promise<void> => {
    vi.useFakeTimers();
    const input = element.querySelector<HTMLInputElement>(
      'input[type="search"]',
    ) as HTMLInputElement;
    input.value = term;
    input.dispatchEvent(new Event('input'));
    await vi.advanceTimersByTimeAsync(300);
    await fixture.whenStable();
    fixture.detectChanges();
    vi.useRealTimers();
  };

  const rowNames = (): string[] =>
    Array.from(element.querySelectorAll('tbody tr .cell-title')).map((cell) =>
      (cell.textContent ?? '').trim(),
    );

  it('should load every type, without filter, sorted by name on opening', async () => {
    const request = nextRequest();

    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('8');
    expect(request.request.params.get('sortBy')).toBe('name');
    expect(request.request.params.has('type')).toBe(false);
    expect(request.request.params.has('search')).toBe(false);
    await respond(request, pageOf([huilesRavo, provenceEssences]));
    expect(rowNames()).toEqual(['Huiles Ravo', 'Provence Essences']);
  });

  it('should reload with the chosen type and show only the returned suppliers', async () => {
    await respond(nextRequest(), pageOf([huilesRavo, provenceEssences]));

    await chooseType('Étranger');

    const request = nextRequest();
    expect(request.request.params.get('type')).toBe('FOREIGN');
    await respond(request, pageOf([provenceEssences]));
    expect(rowNames()).toEqual(['Provence Essences']);
  });

  it('should drop the type when "all types" is chosen again', async () => {
    await respond(nextRequest(), pageOf([huilesRavo]));
    await chooseType('Local');
    await respond(nextRequest(), pageOf([huilesRavo]));

    await chooseType('Tous les types');

    const request = nextRequest();
    expect(request.request.params.has('type')).toBe(false);
    await respond(request, pageOf([huilesRavo, provenceEssences]));
  });

  it('should combine the trimmed search with the chosen type', async () => {
    await respond(nextRequest(), pageOf([huilesRavo]));
    await chooseType('Local');
    await respond(nextRequest(), pageOf([huilesRavo]));

    await searchFor('  Rasoa ');

    const request = nextRequest();
    expect(request.request.params.get('search')).toBe('Rasoa');
    expect(request.request.params.get('type')).toBe('LOCAL');
    await respond(request, pageOf([huilesRavo]));
  });

  it('should go back to the first page when a filter changes', async () => {
    await respond(nextRequest(), pageOf([huilesRavo], 1, 20));
    element.querySelector<HTMLButtonElement>('button[aria-label="Page suivante"]')?.click();
    await fixture.whenStable();
    const second = nextRequest();
    expect(second.request.params.get('page')).toBe('2');
    await respond(second, pageOf([provenceEssences], 2, 20));

    await chooseType('Étranger');

    const request = nextRequest();
    expect(request.request.params.get('page')).toBe('1');
    await respond(request, pageOf([provenceEssences]));
  });

  it('should say that nothing matches when a filter returns no supplier', async () => {
    await respond(nextRequest(), pageOf([huilesRavo]));

    await searchFor('Introuvable');
    await respond(nextRequest(), pageOf([]));

    expect(element.querySelector('.state')?.textContent).toContain('Aucun fournisseur trouvé');
  });

  it('should say that there is no supplier yet when the unfiltered list is empty', async () => {
    await respond(nextRequest(), pageOf([]));

    expect(element.querySelector('.state .title')?.textContent?.trim()).toBe('Aucun fournisseur');
  });

  it('should ignore the answer of a request overtaken by a newer filter', async () => {
    await respond(nextRequest(), pageOf([huilesRavo, provenceEssences]));
    await chooseType('Local');
    const overtaken = nextRequest();

    await chooseType('Étranger');

    expect(overtaken.cancelled).toBe(true);
    await respond(nextRequest(), pageOf([provenceEssences]));
    expect(rowNames()).toEqual(['Provence Essences']);
  });

  it('should offer a retry when the list cannot be loaded', async () => {
    nextRequest().flush(
      { title: 'Forbidden', status: 403, detail: 'Accès refusé', code: 'FORBIDDEN' },
      { status: 403, statusText: 'Forbidden' },
    );
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('.state[role="alert"]')).not.toBeNull();
  });

  it('should show the new supplier button only with the create permission', async () => {
    await respond(nextRequest(), pageOf([huilesRavo]));
    const button = (): HTMLAnchorElement | undefined =>
      Array.from(element.querySelectorAll<HTMLAnchorElement>('a')).find((link) =>
        link.textContent?.includes('Nouveau fournisseur'),
      );
    expect(button()).toBeDefined();

    TestBed.inject(AuthStore).signIn('jeton', buildUser('SELLER'));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(button()).toBeUndefined();
  });

  it('should open the supplier sheet of the clicked row', async () => {
    await respond(nextRequest(), pageOf([huilesRavo, provenceEssences]));

    element.querySelectorAll<HTMLButtonElement>('.row-link')[1].click();
    await fixture.whenStable();
    fixture.detectChanges();

    httpMock.expectOne(`${SUPPLIERS_URL}/${provenceEssences.id}`).flush(provenceEssences);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(element.querySelector('vvs-supplier-detail h2')?.textContent).toContain(
      'Provence Essences',
    );
  });
});
