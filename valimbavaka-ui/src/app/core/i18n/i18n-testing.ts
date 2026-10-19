import { Provider } from '@angular/core';
import { provideTranslateLoader, provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import fr from '../../../../public/i18n/fr.json';

/** Traductions françaises chargées sans HTTP : à n'utiliser que dans les tests. */
export function provideTestTranslations(): Provider[] {
  return provideTranslateService({
    lang: 'fr',
    fallbackLang: 'fr',
    loader: provideTranslateLoader(() => ({ getTranslation: () => of(fr) })),
  });
}
