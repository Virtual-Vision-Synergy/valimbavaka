import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStore } from '../../auth/auth-store';
import { buildUser } from '../../auth/auth-testing';
import { provideTestTranslations } from '../../i18n/i18n-testing';
import { MainLayout } from './main-layout';

/** SYS-04 : le bouton de la barre supérieure bascule tout l'écran entre thème clair et sombre. */
describe('MainLayout theme toggle', () => {
  const html = document.documentElement;
  let fixture: ComponentFixture<MainLayout>;

  const themeButton = (): HTMLButtonElement =>
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      'header.bar button[aria-pressed]',
    ) as HTMLButtonElement;

  beforeEach(async () => {
    localStorage.clear();
    localStorage.setItem('vvs.theme', 'light');
    html.classList.remove('dark');
    await TestBed.configureTestingModule({
      imports: [MainLayout],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTestTranslations(),
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('SELLER'));
    fixture = TestBed.createComponent(MainLayout);
    await fixture.whenStable();
  });

  afterEach(() => {
    html.classList.remove('dark');
    localStorage.clear();
  });

  it('should offer the dark theme while the interface is light', () => {
    expect(themeButton().getAttribute('aria-label')).toBe('Passer au thème sombre');
    expect(themeButton().getAttribute('aria-pressed')).toBe('false');
    expect(html.classList.contains('dark')).toBe(false);
  });

  it('should switch the whole interface to dark immediately on click', async () => {
    themeButton().click();
    await fixture.whenStable();

    expect(html.classList.contains('dark')).toBe(true);
    expect(themeButton().getAttribute('aria-pressed')).toBe('true');
    expect(themeButton().getAttribute('aria-label')).toBe('Passer au thème clair');
    expect(localStorage.getItem('vvs.theme')).toBe('dark');
  });

  it('should come back to light on a second click', async () => {
    themeButton().click();
    await fixture.whenStable();
    themeButton().click();
    await fixture.whenStable();

    expect(html.classList.contains('dark')).toBe(false);
    expect(themeButton().getAttribute('aria-label')).toBe('Passer au thème sombre');
  });
});
