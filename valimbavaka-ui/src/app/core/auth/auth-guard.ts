import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from './auth-service';

/** Confort d'affichage uniquement : le serveur reste seul juge de l'accès (§10.4). */
export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(AuthService)
    .restoreSession()
    .pipe(map((isAuthenticated) => isAuthenticated || router.createUrlTree(['/login'])));
};

/** Page de connexion : un utilisateur déjà connecté est renvoyé vers son accueil. */
export const guestGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(AuthService)
    .restoreSession()
    .pipe(map((isAuthenticated) => !isAuthenticated || router.createUrlTree(['/'])));
};
