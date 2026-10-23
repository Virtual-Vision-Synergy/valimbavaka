import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from './auth-service';
import { AuthStore } from './auth-store';

/**
 * Confort d'affichage uniquement : le serveur reste seul juge de l'accès (§10.4). Un mot de passe provisoire
 * renvoie vers son changement (le serveur n'accorde de toute façon aucune permission avant).
 */
export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const authStore = inject(AuthStore);
  return inject(AuthService)
    .restoreSession()
    .pipe(
      map((isAuthenticated) => {
        if (!isAuthenticated) {
          return router.createUrlTree(['/login']);
        }
        return authStore.currentUser()?.passwordChangeRequired
          ? router.createUrlTree(['/password'])
          : true;
      }),
    );
};

/** Page de changement du mot de passe provisoire : réservée à un compte connecté qui doit le changer. */
export const passwordChangeGuard: CanActivateFn = () => {
  const router = inject(Router);
  const authStore = inject(AuthStore);
  return inject(AuthService)
    .restoreSession()
    .pipe(
      map((isAuthenticated) => {
        if (!isAuthenticated) {
          return router.createUrlTree(['/login']);
        }
        return authStore.currentUser()?.passwordChangeRequired || router.createUrlTree(['/']);
      }),
    );
};

/** Page de connexion : un utilisateur déjà connecté est renvoyé vers son accueil. */
export const guestGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(AuthService)
    .restoreSession()
    .pipe(map((isAuthenticated) => !isAuthenticated || router.createUrlTree(['/'])));
};
