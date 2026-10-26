import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ArticleListPage } from './article-list-page';

describe('ArticleListPage', () => {
  let fixture: ComponentFixture<ArticleListPage>;

  const open = async (role: 'ADMIN' | 'SELLER'): Promise<HTMLElement> => {
    await TestBed.configureTestingModule({
      imports: [ArticleListPage],
      providers: [provideRouter([]), provideTestTranslations()],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser(role));
    fixture = TestBed.createComponent(ArticleListPage);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  };

  const newArticleLink = (element: HTMLElement): HTMLAnchorElement | undefined =>
    Array.from(element.querySelectorAll<HTMLAnchorElement>('a')).find((link) =>
      link.textContent?.includes('Nouvel article'),
    );

  it('should link the new article button to the article sheet for an administrator', async () => {
    const element = await open('ADMIN');

    expect(newArticleLink(element)).toBeDefined();
  });

  it('should hide the new article button without the create permission', async () => {
    const element = await open('SELLER');

    expect(newArticleLink(element)).toBeUndefined();
  });
});
