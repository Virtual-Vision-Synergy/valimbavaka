import { HttpHeaders, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { ArticleListPage } from './article-list-page';
import {
  ARTICLES_URL,
  RANGES_URL,
  botanika,
  pageOf,
  savonNoir,
} from './article-list-page.fixtures';

const EXPORT_URL = `${ARTICLES_URL}/exportable/export`;

/** Bouton « Exporter » de la liste des articles (ART-08) : administrateur seulement, mêmes critères que la liste. */
describe('ArticleListPage export', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<ArticleListPage>;
  let element: HTMLElement;
  let anchorClick: ReturnType<typeof vi.spyOn>;

  const open = async (role: 'ADMIN' | 'SELLER'): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [ArticleListPage],
      providers: [
        provideHttpClient(),
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
    httpMock.expectOne((req) => req.url === ARTICLES_URL).flush(pageOf([savonNoir], 1, 1));
    await fixture.whenStable();
    fixture.detectChanges();
  };

  const exportButton = (): HTMLButtonElement | undefined =>
    Array.from(element.querySelectorAll<HTMLButtonElement>('button')).find(
      (button) => button.textContent?.trim() === 'Exporter',
    );

  beforeEach(() => {
    URL.createObjectURL = vi.fn(() => 'blob:articles');
    URL.revokeObjectURL = vi.fn();
    anchorClick = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);
  });

  afterEach(() => {
    httpMock.verify();
    anchorClick.mockRestore();
  });

  it("n'affiche pas le bouton à la vendeuse", async () => {
    await open('SELLER');

    expect(exportButton()).toBeUndefined();
  });

  it("télécharge le fichier annoncé par le serveur, sans page ni taille dans l'URL", async () => {
    await open('ADMIN');

    exportButton()?.click();
    const request = httpMock.expectOne((req) => req.url === EXPORT_URL);
    request.flush(new Blob(['xlsx']), {
      headers: new HttpHeaders({ 'Content-Disposition': 'attachment; filename="articles.xlsx"' }),
    });
    await fixture.whenStable();

    expect(request.request.method).toBe('GET');
    expect(request.request.params.has('page')).toBe(false);
    expect(request.request.params.has('size')).toBe(false);
    expect(anchorClick).toHaveBeenCalledTimes(1);
    expect(exportButton()?.disabled).toBe(false);
  });

  it('prévient quand le serveur refuse ou échoue, et rend la main', async () => {
    await open('ADMIN');

    exportButton()?.click();
    httpMock
      .expectOne((req) => req.url === EXPORT_URL)
      .flush(new Blob(), { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(
      TestBed.inject(ToastService)
        .toasts()
        .map((toast) => toast.messageKey),
    ).toContain('articles.exportFailed');
    expect(anchorClick).not.toHaveBeenCalled();
    expect(exportButton()?.disabled).toBe(false);
  });
});
