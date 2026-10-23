import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import axe from 'axe-core';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { PasswordChangePage } from './password-change-page';

/** Contrôle axe (WCAG 2.2 AA) de la page de changement de mot de passe ; le contraste exige un vrai navigateur. */
describe('PasswordChangePage accessibility', () => {
  it('should have no detectable accessibility violation', async () => {
    await TestBed.configureTestingModule({
      imports: [PasswordChangePage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(PasswordChangePage);
    await fixture.whenStable();
    document.body.appendChild(fixture.nativeElement as HTMLElement);

    const results = await axe.run(fixture.nativeElement as HTMLElement, {
      runOnly: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'],
      rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
    });

    expect(results.violations.map((violation) => violation.id)).toEqual([]);
  });
});
