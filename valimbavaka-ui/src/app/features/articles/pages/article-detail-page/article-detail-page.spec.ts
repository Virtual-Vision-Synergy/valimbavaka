import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ARTICLES_FIXTURE } from '../../data-access/article-fixtures';
import { ArticleDetailPage } from './article-detail-page';

describe('ArticleDetailPage', () => {
  let fixture: ComponentFixture<ArticleDetailPage>;
  const demoArticle = ARTICLES_FIXTURE[0];

  const open = async (role: 'ADMIN' | 'SELLER'): Promise<HTMLElement> => {
    await TestBed.configureTestingModule({
      imports: [ArticleDetailPage],
      providers: [provideRouter([]), provideTestTranslations()],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser(role));
    fixture = TestBed.createComponent(ArticleDetailPage);
    fixture.componentRef.setInput('id', demoArticle.id);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  };

  const editLink = (element: HTMLElement): HTMLAnchorElement | undefined =>
    Array.from(element.querySelectorAll<HTMLAnchorElement>('a')).find((link) =>
      link.textContent?.includes('Modifier'),
    );

  it('should link the edit button to the edit form for an administrator', async () => {
    const element = await open('ADMIN');

    expect(editLink(element)?.getAttribute('href')).toBe(`/articles/${demoArticle.id}/edit`);
  });

  it('should hide the edit button without the update permission', async () => {
    const element = await open('SELLER');

    expect(element.querySelector('h1')).not.toBeNull();
    expect(editLink(element)).toBeUndefined();
  });
});
