import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';

export const SUPPORTED_LANGUAGES = ['fr', 'mg'] as const;
export type AppLanguage = (typeof SUPPORTED_LANGUAGES)[number];

const DEFAULT_LANGUAGE: AppLanguage = 'fr';
const STORAGE_KEY = 'vvs.lang';

/** Langue de l'interface (fr, mg) : simple préférence d'affichage, mémorisée dans le navigateur. */
@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly translate = inject(TranslateService);
  private readonly document = inject(DOCUMENT);

  private readonly selected = signal<AppLanguage>(DEFAULT_LANGUAGE);
  readonly current = this.selected.asReadonly();

  /** Charge la langue mémorisée (ou le français) ; appelé avant l'affichage de l'application. */
  init(): Observable<unknown> {
    return this.apply(this.storedLanguage());
  }

  use(language: AppLanguage): void {
    try {
      // eslint-disable-next-line no-restricted-globals -- préférence d'affichage, aucune donnée sensible (§9)
      localStorage.setItem(STORAGE_KEY, language);
    } catch {
      // Stockage indisponible (navigation privée) : la préférence vaut pour la session seulement.
    }
    this.apply(language).subscribe();
  }

  private apply(language: AppLanguage): Observable<unknown> {
    this.selected.set(language);
    this.document.documentElement.lang = language;
    return this.translate.use(language);
  }

  private storedLanguage(): AppLanguage {
    try {
      // eslint-disable-next-line no-restricted-globals -- préférence d'affichage, aucune donnée sensible (§9)
      const stored = localStorage.getItem(STORAGE_KEY);
      return SUPPORTED_LANGUAGES.find((language) => language === stored) ?? DEFAULT_LANGUAGE;
    } catch {
      return DEFAULT_LANGUAGE;
    }
  }
}
