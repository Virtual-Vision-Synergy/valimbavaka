import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { TitleStrategy, provideRouter, withComponentInputBinding } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { firstValueFrom } from 'rxjs';
import { routes } from './app.routes';
import { authInterceptor } from './core/http/auth-interceptor';
import { errorInterceptor } from './core/http/error-interceptor';
import { sessionInterceptor } from './core/http/session-interceptor';
import { LanguageService } from './core/i18n/language-service';
import { TranslatedTitleStrategy } from './core/i18n/translated-title-strategy';
import { UiPreferencesService } from './core/layout/ui-preferences-service';
import { OutboxService } from './core/offline/outbox-service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes, withComponentInputBinding()),
    { provide: TitleStrategy, useExisting: TranslatedTitleStrategy },
    provideTranslateService({
      fallbackLang: 'fr',
      loader: provideTranslateHttpLoader({ prefix: '/i18n/', suffix: '.json' }),
    }),
    provideAppInitializer(() => firstValueFrom(inject(LanguageService).init())),
    // Instancie la file d'attente hors ligne dès le démarrage : elle se synchronise seule (SYS-02).
    provideAppInitializer(() => void inject(OutboxService)),
    // Applique le thème mémorisé (ou celui du système) avant le premier rendu.
    provideAppInitializer(() => void inject(UiPreferencesService)),
    provideHttpClient(
      withFetch(),
      withInterceptors([authInterceptor, sessionInterceptor, errorInterceptor]),
    ),
  ],
};
