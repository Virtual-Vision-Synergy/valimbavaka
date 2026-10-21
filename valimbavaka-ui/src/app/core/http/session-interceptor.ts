import { HttpEvent, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { Injector, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, switchMap, throwError } from 'rxjs';
import { AUTH_URL_PREFIX, AuthService } from '../auth/auth-service';
import { AuthStore } from '../auth/auth-store';
import { ApiError } from './problem-detail';

const API_PREFIX = '/api/';
const HTTP_UNAUTHORIZED = 401;

interface SessionRetryContext {
  readonly authService: AuthService;
  readonly authStore: AuthStore;
  readonly injector: Injector;
  readonly next: HttpHandlerFn;
  readonly originalError: ApiError;
  readonly request: HttpRequest<unknown>;
  readonly requestRevision: number;
  readonly requestUserId: string | null;
}

/**
 * Sur 401 d'un appel d'API, tente un seul refresh puis rejoue la requête ;
 * si le refresh échoue, la session est fermée et l'utilisateur renvoyé vers la connexion (§10.4).
 * Doit être placé avant errorInterceptor dans la liste : il reçoit des ApiError.
 * Le Router est résolu à la demande pour éviter une dépendance circulaire au démarrage.
 */
export const sessionInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const authStore = inject(AuthStore);
  const injector = inject(Injector);
  const requestUserId = authStore.currentUser()?.id ?? null;
  const requestRevision = authStore.sessionRevision();
  const isRetryable =
    request.url.startsWith(API_PREFIX) && !request.url.startsWith(AUTH_URL_PREFIX);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (!isUnauthorizedApiError(error, isRetryable)) {
        return throwError(() => error);
      }
      return retryUnauthorizedRequest({
        authService,
        authStore,
        injector,
        next,
        originalError: error,
        request,
        requestRevision,
        requestUserId,
      });
    }),
  );
};

function isUnauthorizedApiError(error: unknown, isRetryable: boolean): error is ApiError {
  return isRetryable && error instanceof ApiError && error.status === HTTP_UNAUTHORIZED;
}

function retryUnauthorizedRequest(context: SessionRetryContext): Observable<HttpEvent<unknown>> {
  if (!isSameAccount(context)) {
    return throwError(() => context.originalError);
  }
  const currentToken = context.authStore.accessToken();
  const requestToken = context.request.headers.get('Authorization')?.replace(/^Bearer /, '');
  if (context.requestUserId && currentToken && requestToken !== currentToken) {
    return context.next(
      context.request.clone({ setHeaders: { Authorization: `Bearer ${currentToken}` } }),
    );
  }
  return refreshAndReplay(context);
}

function refreshAndReplay(context: SessionRetryContext): Observable<HttpEvent<unknown>> {
  return context.authService.refresh(context.requestUserId ?? undefined).pipe(
    catchError((refreshError: unknown) => handleRefreshFailure(context, refreshError)),
    switchMap((response) => {
      if (
        !isSameAccount(context) ||
        (context.requestUserId && response.user.id !== context.requestUserId)
      ) {
        return throwError(() => context.originalError);
      }
      return context.next(
        context.request.clone({ setHeaders: { Authorization: `Bearer ${response.accessToken}` } }),
      );
    }),
  );
}

function handleRefreshFailure(context: SessionRetryContext, error: unknown): Observable<never> {
  if (isSameSession(context)) {
    context.authStore.signOut();
    void context.injector.get(Router).navigate(['/login']);
  }
  return throwError(() => error);
}

function isSameAccount(context: SessionRetryContext): boolean {
  return (context.authStore.currentUser()?.id ?? null) === context.requestUserId;
}

function isSameSession(context: SessionRetryContext): boolean {
  return isSameAccount(context) && context.authStore.sessionRevision() === context.requestRevision;
}
