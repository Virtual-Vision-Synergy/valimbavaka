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
import { ArticleImportRDto } from '../../../article/data-access/article-models';
import { ArticleListPage } from './article-list-page';
import {
  ARTICLES_URL,
  RANGES_URL,
  botanika,
  huileRicin,
  pageOf,
  savonNoir,
} from './article-list-page.fixtures';

const IMPORT_URL = `${ARTICLES_URL}/import`;

const withConflict: ArticleImportRDto = {
  created: 1,
  updated: 0,
  unchanged: 0,
  conflicts: 1,
  invalid: 1,
  rows: [
    { row: 1, status: 'CREATED', code: 'ART-0040', message: null },
    {
      row: 2,
      status: 'CONFLICT',
      code: 'ART-0001',
      message: "L'article ART-0001 · Savon noir 100 g existe déjà (même code ou même code-barres).",
    },
    { row: 3, status: 'INVALID', code: null, message: 'Le nom est obligatoire' },
  ],
};

/** Import CSV en masse depuis la liste des articles (ART-07). */
describe('ArticleListPage — import CSV', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<ArticleListPage>;
  let element: HTMLElement;

  const open = async (role: 'ADMIN' | 'SELLER'): Promise<void> => {
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
    httpMock.expectOne((req) => req.url === RANGES_URL).flush({ items: [botanika] });
    await settle(httpMock.expectOne((req) => req.url === ARTICLES_URL));
  };

  const settle = async (request: TestRequest, body: object = pageOf([savonNoir, huileRicin])) => {
    request.flush(body);
    await fixture.whenStable();
    fixture.detectChanges();
  };

  const pickFile = (): void => {
    const input = element.querySelector<HTMLInputElement>('input[type="file"]') as HTMLInputElement;
    const file = new File(['name;type;unit;threshold\n'], 'articles.csv', { type: 'text/csv' });
    Object.defineProperty(input, 'files', { value: { item: () => file } });
    input.dispatchEvent(new Event('change'));
  };

  const importRequest = (mode: string): TestRequest =>
    httpMock.expectOne((req) => req.url === IMPORT_URL && req.params.get('onConflict') === mode);

  afterEach(() => httpMock.verify());

  it("affiche le bouton d'import à l'administrateur", async () => {
    await open('ADMIN');

    expect(element.textContent).toContain('Importer');
    expect(element.querySelector('input[type="file"]')).not.toBeNull();
  });

  it("n'affiche pas le bouton d'import à la vendeuse", async () => {
    await open('SELLER');

    expect(element.textContent).not.toContain('Importer');
  });

  it('envoie le fichier choisi puis affiche le compte rendu avec les lignes rejetées', async () => {
    await open('ADMIN');

    pickFile();
    await settle(importRequest('SKIP'), withConflict);
    await settle(httpMock.expectOne((req) => req.url === ARTICLES_URL));

    expect(element.querySelector('vvs-article-import-report')).not.toBeNull();
    expect(element.textContent).toContain('1 créé(s)');
    expect(element.textContent).toContain('Le nom est obligatoire');
    expect(element.textContent).toContain('Écraser les 1 article(s) en conflit');
  });

  it("rejoue uniquement les lignes en conflit quand l'administrateur choisit d'écraser", async () => {
    await open('ADMIN');
    pickFile();
    await settle(importRequest('SKIP'), withConflict);
    await settle(httpMock.expectOne((req) => req.url === ARTICLES_URL));

    const overwrite = Array.from(element.querySelectorAll<HTMLButtonElement>('button')).find(
      (button) => button.textContent?.includes('Écraser'),
    ) as HTMLButtonElement;
    overwrite.click();

    const replay = importRequest('OVERWRITE');
    expect(replay.request.params.get('onlyRows')).toBe('2');
    await settle(replay, { ...withConflict, conflicts: 0, updated: 1, rows: [] });
    await settle(httpMock.expectOne((req) => req.url === ARTICLES_URL));
    expect(element.textContent).not.toContain('Écraser les');
  });

  it("n'écrase rien tant que l'administrateur ne le demande pas", async () => {
    await open('ADMIN');

    pickFile();
    await settle(importRequest('SKIP'), withConflict);
    await settle(httpMock.expectOne((req) => req.url === ARTICLES_URL));

    httpMock.expectNone((req) => req.url === IMPORT_URL);
  });

  it("signale l'échec d'un fichier refusé sans afficher de compte rendu", async () => {
    await open('ADMIN');

    pickFile();
    const request = importRequest('SKIP');
    request.flush(
      { status: 422, code: 'VALIDATION_FAILED', detail: 'Le fichier dépasse 1000 lignes.' },
      { status: 422, statusText: 'Unprocessable Content' },
    );
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('vvs-article-import-report')).toBeNull();
  });
});
