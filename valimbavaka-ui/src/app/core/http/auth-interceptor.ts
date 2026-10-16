import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthStore } from '../auth/auth-store';

const API_PREFIX = '/api/';

/** Ajoute le jeton d'accès aux appels de notre API uniquement, jamais vers un domaine tiers. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthStore).accessToken();
  const isApiCall = req.url.startsWith(API_PREFIX);

  return next(
    token && isApiCall ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req,
  );
};
