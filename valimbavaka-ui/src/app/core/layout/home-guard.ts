import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from '../auth/auth-store';
import { visibleSections } from './menu';

/**
 * Accueil : un profil terrain (livraisons sans vente) ouvre sa tournée mobile ; les autres, le
 * premier module autorisé du menu (le tableau de bord pour les rôles actuels).
 */
export const homeGuard: CanActivateFn = () => {
  const authStore = inject(AuthStore);
  const router = inject(Router);
  if (authStore.hasPermission('deliveries.read') && !authStore.hasPermission('sales.read')) {
    return router.createUrlTree(['/round']);
  }
  const firstItem = visibleSections((permission) => authStore.hasPermission(permission))[0]
    ?.items[0];
  return router.createUrlTree([firstItem?.route ?? '/no-access']);
};
