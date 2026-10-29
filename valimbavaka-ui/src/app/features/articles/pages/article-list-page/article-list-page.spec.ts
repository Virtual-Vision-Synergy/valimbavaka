import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ArticleListPage } from './article-list-page';
import {
  ARTICLES_URL,
  RANGES_URL,
  RANGE_ID,
  botanika,
  huileRicin,
  pageOf,
  savonNoir,
  zezika,
} from './article-list-page.fixtures';

/** Liste des articles de la maquette, branchée sur `/api/v1/articles` (ART-05). */
describe('ArticleListPage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<ArticleListPage>;
  let element: HTMLElement;

  const open = async (role: 'ADMIN' | 'SELLER' = 'ADMIN'): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [ArticleListPage],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser(role));
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ArticleListPage);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  };

  afterEach(() => {
    httpMock.verify();
  });

  const nextRequest = (): TestRequest => httpMock.expectOne((req) => req.url === ARTICLES_URL);

  const respond = async (request: TestRequest, body: object): Promise<void> => {
    request.flush(body);
    await fixture.whenStable();
    fixture.detectChanges();
  };

  /** Ouvre la page puis sert les gammes et la première page d'articles. */
  const openLoaded = async (items = [savonNoir, huileRicin], totalCount = items.length) => {
    await open();
    httpMock.expectOne(RANGES_URL).flush({ items: [botanika, zezika] });
    await respond(nextRequest(), pageOf(items, 1, totalCount));
  };

  /** Une requête lancée par la liste est une tâche en cours tant que le test ne l'a pas fait répondre. */
  const refresh = async (): Promise<void> => {
    fixture.detectChanges();
    await Promise.resolve();
    fixture.detectChanges();
  };

  const chooseRange = async (label: string): Promise<void> => {
    Array.from(element.querySelectorAll<HTMLButtonElement>('.chips button'))
      .find((chip) => chip.textContent?.includes(label))
      ?.click();
    await refresh();
  };

  const chooseType = async (value: string): Promise<void> => {
    const select = element.querySelector<HTMLSelectElement>('select') as HTMLSelectElement;
    select.value = value;
    select.dispatchEvent(new Event('change'));
    await refresh();
  };

  /** La recherche part 300 ms après la dernière frappe : on attend ce délai en temps réel. */
  const searchFor = async (term: string): Promise<void> => {
    const input = element.querySelector<HTMLInputElement>(
      'input[type="search"]',
    ) as HTMLInputElement;
    input.value = term;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await new Promise((resolve) => setTimeout(resolve, 350));
    await refresh();
  };

  const rowNames = (): string[] =>
    Array.from(element.querySelectorAll('tbody tr .cell-title')).map((cell) =>
      (cell.textContent ?? '').trim(),
    );

  const newArticleLink = (): HTMLAnchorElement | undefined =>
    Array.from(element.querySelectorAll<HTMLAnchorElement>('a')).find((link) =>
      link.textContent?.includes('Nouvel article'),
    );

  it('should load the first page sorted by name, without criteria, on opening', async () => {
    await open();
    httpMock.expectOne(RANGES_URL).flush({ items: [botanika, zezika] });
    const request = nextRequest();

    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('10');
    expect(request.request.params.get('sortBy')).toBe('name');
    for (const key of ['search', 'type', 'rangeId']) {
      expect(request.request.params.has(key)).toBe(false);
    }
    await respond(request, pageOf([savonNoir, huileRicin]));
    expect(rowNames()).toEqual(['Savon noir 100 g', 'Huile de ricin']);
  });

  it('should show the code and the active state of each article', async () => {
    await openLoaded();

    const text = element.querySelector('tbody')?.textContent ?? '';
    expect(text).toContain('ART-0031');
    expect(text).toContain('Actif');
    expect(text).toContain('Inactif');
  });

  it('should reload with the trimmed search once typing has paused', async () => {
    await openLoaded();

    await searchFor('  savon ');

    const request = nextRequest();
    expect(request.request.params.get('search')).toBe('savon');
    await respond(request, pageOf([savonNoir]));
    expect(rowNames()).toEqual(['Savon noir 100 g']);
  });

  it('should not query again while the search only differs by spaces', async () => {
    await openLoaded();
    await searchFor('savon');
    await respond(nextRequest(), pageOf([savonNoir]));

    await searchFor('savon  ');

    httpMock.expectNone(ARTICLES_URL);
  });

  it('should reload with the chosen range id and drop it for all ranges', async () => {
    await openLoaded();

    await chooseRange('Botanika');
    const ranged = nextRequest();
    expect(ranged.request.params.get('rangeId')).toBe(RANGE_ID);
    await respond(ranged, pageOf([savonNoir]));

    await chooseRange('Toutes gammes');
    const all = nextRequest();
    expect(all.request.params.has('rangeId')).toBe(false);
    await respond(all, pageOf([savonNoir, huileRicin]));
  });

  it('should reload with the chosen type and show only the returned articles', async () => {
    await openLoaded();

    await chooseType('RAW_MATERIAL');

    const request = nextRequest();
    expect(request.request.params.get('type')).toBe('RAW_MATERIAL');
    await respond(request, pageOf([huileRicin]));
    expect(rowNames()).toEqual(['Huile de ricin']);
  });

  it('should combine search, range and type in a single request', async () => {
    await openLoaded();
    await chooseRange('Botanika');
    await respond(nextRequest(), pageOf([savonNoir]));
    await chooseType('FINISHED');
    await respond(nextRequest(), pageOf([savonNoir]));

    await searchFor('savon');

    const { params } = nextRequest().request;
    expect([params.get('search'), params.get('rangeId'), params.get('type')]).toEqual([
      'savon',
      RANGE_ID,
      'FINISHED',
    ]);
  });

  it('should go back to the first page when a criterion changes', async () => {
    await openLoaded([savonNoir], 25);
    element.querySelector<HTMLButtonElement>('button[aria-label="Page suivante"]')?.click();
    await refresh();
    const second = nextRequest();
    expect(second.request.params.get('page')).toBe('2');
    await respond(second, pageOf([huileRicin], 2, 25));

    await chooseType('FINISHED');

    const request = nextRequest();
    expect(request.request.params.get('page')).toBe('1');
    await respond(request, pageOf([savonNoir], 1, 25));
  });

  it('should ignore the answer of a request overtaken by a newer criterion', async () => {
    await openLoaded();
    await chooseType('FINISHED');
    const overtaken = nextRequest();

    await chooseType('RAW_MATERIAL');

    expect(overtaken.cancelled).toBe(true);
    await respond(nextRequest(), pageOf([huileRicin]));
    expect(rowNames()).toEqual(['Huile de ricin']);
  });

  it('should show the empty state when no article matches', async () => {
    await openLoaded();

    await searchFor('zzz');
    await respond(nextRequest(), pageOf([]));

    expect(element.querySelector('tbody')).toBeNull();
    expect(element.querySelector('.state')).not.toBeNull();
  });

  it('should keep the list usable when the ranges cannot be loaded', async () => {
    await open();
    httpMock
      .expectOne(RANGES_URL)
      .flush(
        { title: 'Forbidden', status: 403, code: 'FORBIDDEN' },
        { status: 403, statusText: 'Forbidden' },
      );
    await respond(nextRequest(), pageOf([savonNoir]));

    expect(rowNames()).toEqual(['Savon noir 100 g']);
  });

  it('should offer a retry when the list cannot be loaded', async () => {
    await open();
    httpMock.expectOne(RANGES_URL).flush({ items: [botanika] });
    nextRequest().flush(
      { title: 'Forbidden', status: 403, detail: 'Accès refusé', code: 'FORBIDDEN' },
      { status: 403, statusText: 'Forbidden' },
    );
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('.state[role="alert"]')).not.toBeNull();
  });

  it('should link the new article button to the article sheet for an administrator', async () => {
    await openLoaded();

    expect(newArticleLink()).toBeDefined();
  });

  it('should hide the new article button without the create permission', async () => {
    await open('SELLER');
    httpMock.expectOne(RANGES_URL).flush({ items: [] });
    await respond(nextRequest(), pageOf([]));

    expect(newArticleLink()).toBeUndefined();
  });
});
