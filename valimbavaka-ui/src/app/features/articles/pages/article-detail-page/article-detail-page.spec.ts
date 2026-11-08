import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ArticleOverviewRDto } from '../../../article/data-access/article-models';
import { SHEET_RANGES, buildOverview } from '../../data-access/article-sheet-testing';
import { ArticleDetailPage } from './article-detail-page';

/** Fiche article 360° (ART-06) : un appel `GET /api/v1/articles/{id}/overview` et les gammes du référentiel. */
describe('ArticleDetailPage', () => {
  let fixture: ComponentFixture<ArticleDetailPage>;
  let httpMock: HttpTestingController;
  const articleId = 'article-soap';

  const open = async (
    role: 'ADMIN' | 'SELLER',
    overview: ArticleOverviewRDto | null = buildOverview(),
  ): Promise<HTMLElement> => {
    await TestBed.configureTestingModule({
      imports: [ArticleDetailPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTestTranslations(),
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthStore).signIn('jeton', buildUser(role));
    fixture = TestBed.createComponent(ArticleDetailPage);
    fixture.componentRef.setInput('id', articleId);
    fixture.detectChanges();
    const request = httpMock.expectOne(`/api/v1/articles/${articleId}/overview`);
    if (overview) {
      request.flush(overview);
    } else {
      request.flush({ code: 'NOT_FOUND' }, { status: 404, statusText: 'Not Found' });
    }
    httpMock
      .match((pending) => pending.url === '/api/v1/product-ranges')
      .filter((pending) => !pending.cancelled)
      .forEach((pending) => pending.flush({ items: SHEET_RANGES, page: 1, pageSize: 100 }));
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  };

  const editLink = (element: HTMLElement): HTMLAnchorElement | undefined =>
    Array.from(element.querySelectorAll<HTMLAnchorElement>('a')).find((link) =>
      link.textContent?.includes('Modifier'),
    );

  const headings = (element: HTMLElement): string[] =>
    Array.from(element.querySelectorAll('h2')).map((title) => title.textContent?.trim() ?? '');

  afterEach(() => httpMock.verify());

  it('should show the four blocks of the sheet without further navigation', async () => {
    const element = await open('ADMIN');
    const text = (element.textContent ?? '').replace(/\s+/g, ' ');

    expect(element.querySelector('h1')?.textContent).toContain('Savon noir 100 g');
    expect(headings(element)).toEqual([
      'Stock par emplacement',
      'Lots de production',
      'Promotions',
      "Journal de l'article",
    ]);
    expect(text).toContain('Boutique');
    expect(text).toContain('LOT-00012');
    expect(text).toContain('Produit : 12 u.');
    expect(text).toContain('Fête des mères');
    expect(text).toContain('−20 %');
    expect(text).toContain('Transfert · Boutique');
    expect(text).toContain('Stock faible');
  });

  it('should link the edit button to the edit form for an administrator', async () => {
    const element = await open('ADMIN');

    expect(editLink(element)?.getAttribute('href')).toBe(`/articles/${articleId}/edit`);
  });

  it('should hide the edit button without the update permission', async () => {
    const element = await open('SELLER');

    expect(element.querySelector('h1')).not.toBeNull();
    expect(editLink(element)).toBeUndefined();
  });

  it('should show an input batch with the product it makes and empty blocks as messages', async () => {
    const overview = buildOverview();
    const element = await open(
      'ADMIN',
      buildOverview({
        article: { ...overview.article, type: 'RAW_MATERIAL', unit: 'KILOGRAM', rangeId: null },
        activePromotions: [],
        recentMovements: [],
        productionBatches: [{ ...overview.productionBatches[0], role: 'INPUT', quantity: 0.45 }],
      }),
    );
    const text = (element.textContent ?? '').replace(/\s+/g, ' ');

    expect(text).toContain('Consommé : 0.45 kg');
    expect(text).toContain('ART-0031 · Savon noir 100 g');
    expect(text).toContain('Aucune promotion active ne vise cet article.');
    expect(text).toContain('Aucun mouvement de stock pour cet article.');
  });

  it('should show the error state when the article is unknown', async () => {
    const element = await open('ADMIN', null);

    expect(element.querySelector('h1')).toBeNull();
    expect(element.querySelector('vvs-list-state')).not.toBeNull();
  });
});
