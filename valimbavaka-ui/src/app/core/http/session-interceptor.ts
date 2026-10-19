import { HttpInterceptorFn } from '@angular/common/http';
import { Injector, inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AUTH_URL_PREFIX, AuthService } from '../auth/auth-service';
import { AuthStore } from '../auth/auth-store';
import { ApiError } from './problem-detail';

const API_PREFIX = '/api/';
const HTTP_UNAUTHORIZED = 401;

/**
 * Sur 401 d'un appel d'API (jeton d'accès expiré), tente un seul refresh puis rejoue la requête ;
 * si le refresh échoue, la session est fermée et l'utilisateur renvoyé vers la connexion (§10.4).
 * Doit être placé avant errorInterceptor dans la liste : il reçoit des ApiError.
 * Le Router est résolu à la demande : l'injecter ici ferait une dépendance circulaire au démarrage
 * (chargement des traductions via HttpClient → Router → TranslatedTitleStrategy → TranslateService).
 */
export const sessionInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const authStore = inject(AuthStore);
  const injector = inject(Injector);
  const isRetryable = req.url.startsWith(API_PREFIX) && !req.url.startsWith(AUTH_URL_PREFIX);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (!isRetryable || !(error instanceof ApiError) || error.status !== HTTP_UNAUTHORIZED) {
        return throwError(() => error);
      }
      return authService.refresh().pipe(
        catchError((refreshError: unknown) => {
          authStore.signOut();
          void injector.get(Router).navigate(['/login']);
          return throwError(() => refreshError);
        }),
        switchMap((response) =>
          next(req.clone({ setHeaders: { Authorization: `Bearer ${response.accessToken}` } })),
        ),
      );
    }),
  );
};
