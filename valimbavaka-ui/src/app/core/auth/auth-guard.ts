import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from './auth-store';

/** Confort d'affichage uniquement : le serveur reste seul juge de l'accès (§10.4). */
export const authGuard: CanActivateFn = () =>
  inject(AuthStore).isAuthenticated() || inject(Router).createUrlTree(['/login']);
