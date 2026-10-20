import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from '../auth/auth-store';
import { visibleSections } from './menu';

/** Accueil : ouvre le premier module autorisé du rôle (le tableau de bord pour tous les rôles actuels). */
export const homeGuard: CanActivateFn = () => {
  const firstItem = visibleSections((permission) => inject(AuthStore).hasPermission(permission))[0]
    ?.items[0];
  return inject(Router).createUrlTree([firstItem?.route ?? '/no-access']);
};
