import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AppLanguage, LanguageService, SUPPORTED_LANGUAGES } from './language-service';

/** Sélecteur de langue accessible (un vrai <select> étiqueté), utilisable partout. */
@Component({
  selector: 'vvs-language-switcher',
  imports: [TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <label class="lang">
      <span class="lang-label">{{ 'language.label' | translate }}</span>
      <select (change)="select($any($event.target).value)">
        @for (language of languages; track language) {
          <option [value]="language" [selected]="language === current()">
            {{ 'language.' + language | translate }}
          </option>
        }
      </select>
    </label>
  `,
  styleUrl: './language-switcher.css',
})
export class LanguageSwitcher {
  private readonly languageService = inject(LanguageService);

  protected readonly languages = SUPPORTED_LANGUAGES;
  protected readonly current = this.languageService.current;

  protected select(language: AppLanguage): void {
    this.languageService.use(language);
  }
}
