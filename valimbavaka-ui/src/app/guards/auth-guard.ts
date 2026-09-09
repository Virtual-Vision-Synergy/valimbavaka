import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  // TODO (feature auth) : vérifier l'état d'authentification réel
  const isAuthenticated = false;

  return isAuthenticated ? true : router.createUrlTree(['/login']);
};
