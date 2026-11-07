import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { ArticleRDto } from '../../../features/article/data-access/article-models';
import { AuthStore } from '../../auth/auth-store';
import { ROLE_PERMISSIONS, buildUser } from '../../auth/auth-testing';
import { provideTestTranslations } from '../../i18n/i18n-testing';
import { SearchDialog } from './search-dialog';

const ARTICLES_URL = '/api/v1/articles';

const savon: ArticleRDto = {
  id: '0195f0a0-0000-7000-8000-0000000000a1',
  code: 'ART-0031',
  name: 'Savon noir 100 g',
  barcode: null,
  type: 'FINISHED',
  rangeId: null,
  unit: 'PIECE',
  cost: 2500,
  price: 12000,
  threshold: 10,
  supplierId: null,
  active: true,
  createdAt: '2026-10-26T08:00:00Z',
  updatedAt: '2026-10-26T08:00:00Z',
  version: 0,
};

const pageOf = (items: ArticleRDto[]) => ({
  items,
  page: 1,
  pageSize: 5,
  totalCount: items.length,
  totalPages: 1,
});

/** Palette de commandes (SYS-03) : pages et créations à chaque frappe, articles lus sur l'API. */
describe('SearchDialog', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<SearchDialog>;
  let element: HTMLElement;
  let closed: number;

  const open = async (role: keyof typeof ROLE_PERMISSIONS = 'ADMIN'): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [SearchDialog],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser(role));
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(SearchDialog);
    element = fixture.nativeElement as HTMLElement;
    closed = 0;
    fixture.componentInstance.closed.subscribe(() => closed++);
    await refresh();
  };

  const refresh = async (): Promise<void> => {
    await fixture.whenStable();
    fixture.detectChanges();
  };

  const field = (): HTMLInputElement =>
    element.querySelector<HTMLInputElement>('#global-search') as HTMLInputElement;

  /** Saisie sans attendre la temporisation : ce qui s'affiche tout de suite. */
  const type = (term: string): void => {
    field().value = term;
    field().dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };

  /**
   * La recherche d'articles part 300 ms après la dernière frappe : on attend ce délai en temps réel.
   * Une requête lancée est une tâche en cours tant que le test ne l'a pas fait répondre : pas de `whenStable`.
   */
  const pause = async (): Promise<void> => {
    await new Promise((resolve) => setTimeout(resolve, 350));
    fixture.detectChanges();
    await Promise.resolve();
    fixture.detectChanges();
  };

  const articlesRequest = (): TestRequest => httpMock.expectOne((req) => req.url === ARTICLES_URL);

  const options = (): string[] =>
    Array.from(element.querySelectorAll('[role="option"] .title')).map(
      (title) => title.textContent?.trim() ?? '',
    );

  const status = (): string => element.querySelector('#search-status')?.textContent?.trim() ?? '';

  afterEach(() => httpMock.verify());

  it('should filter pages and creations at each keystroke, before any server call', async () => {
    await open();

    type('fourn');

    expect(options()).toEqual(['Créer un fournisseur', 'Catalogue › Fournisseurs']);
    httpMock.expectNone((req) => req.url === ARTICLES_URL);
    await pause();
    articlesRequest().flush(pageOf([]));
  });

  it('should ask the server for one page of articles once typing has paused', async () => {
    await open();

    type('  savon ');
    await pause();

    const request = articlesRequest();
    expect(request.request.params.get('search')).toBe('savon');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('5');
    expect(request.request.params.get('sortBy')).toBe('name');
    request.flush(pageOf([savon]));
    await refresh();

    expect(options()).toEqual(['Savon noir 100 g']);
    expect(element.querySelector('[role="option"] mark')?.textContent).toBe('Savon');
    expect(status()).toBe('1 résultat(s)');
  });

  it('should not call the server below two characters', async () => {
    await open();

    type(' s ');
    await pause();

    httpMock.expectNone((req) => req.url === ARTICLES_URL);
    expect(options()).toEqual([]);
    expect(status()).toContain('au moins 2 caractères');
  });

  it('should open the chosen article with Enter', async () => {
    await open();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    type('savon');
    await pause();
    articlesRequest().flush(pageOf([savon]));
    await refresh();

    field().dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

    expect(navigate).toHaveBeenCalledWith(`/articles/${savon.id}`);
    expect(closed).toBe(1);
  });

  it('should move the active result with the arrow keys', async () => {
    await open();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    // jsdom ne fait pas défiler : on observe seulement que l'option active est ramenée à l'écran.
    const scrollIntoView = vi.fn();
    Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', {
      value: scrollIntoView,
      configurable: true,
    });
    type('fourn');
    await pause();
    articlesRequest().flush(pageOf([]));
    await refresh();

    field().dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown' }));
    fixture.detectChanges();
    field().dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

    expect(scrollIntoView).toHaveBeenCalledWith({ block: 'nearest' });
    expect(navigate).toHaveBeenCalledWith('/suppliers');
    Reflect.deleteProperty(HTMLElement.prototype, 'scrollIntoView');
  });

  it('should keep pages and actions when the articles cannot be read', async () => {
    await open();

    type('article');
    await pause();
    articlesRequest().error(new ProgressEvent('error'));
    await refresh();

    expect(options()).toEqual(['Créer un article', 'Catalogue › Articles']);
    expect(status()).toContain('Articles indisponibles');
  });

  it('should not search articles for a delivery driver', async () => {
    await open('DELIVERY_DRIVER');

    type('savon');
    await pause();

    httpMock.expectNone((req) => req.url === ARTICLES_URL);
    expect(status()).toBe('Aucun résultat pour « savon ».');
  });

  it('should offer a seller no administrator creation', async () => {
    await open('SELLER');

    type('créer');
    await pause();
    articlesRequest().flush(pageOf([]));
    await refresh();

    expect(options()).toEqual([]);
  });

  it('should emit closed when the close button is pressed', async () => {
    await open();

    element.querySelector<HTMLButtonElement>('button.close')?.click();

    expect(closed).toBe(1);
  });
});
