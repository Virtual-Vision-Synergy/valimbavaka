import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { provideTestTranslations } from './i18n-testing';
import { LanguageService } from './language-service';

describe('LanguageService', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideTestTranslations()] });
  });

  it('should start in French when nothing is stored', () => {
    TestBed.inject(LanguageService).init().subscribe();

    expect(TestBed.inject(LanguageService).current()).toBe('fr');
    expect(document.documentElement.lang).toBe('fr');
  });

  it('should remember the chosen language and tag the document with it', () => {
    const sut = TestBed.inject(LanguageService);

    sut.use('mg');

    expect(sut.current()).toBe('mg');
    expect(document.documentElement.lang).toBe('mg');
    expect(localStorage.getItem('vvs.lang')).toBe('mg');
    expect(TestBed.inject(TranslateService).getCurrentLang()).toBe('mg');
  });

  it('should ignore an unsupported stored language', () => {
    localStorage.setItem('vvs.lang', 'xx');
    const sut = TestBed.inject(LanguageService);

    sut.init().subscribe();

    expect(sut.current()).toBe('fr');
  });
});
