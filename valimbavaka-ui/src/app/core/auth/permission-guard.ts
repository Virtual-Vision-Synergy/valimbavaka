import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from './auth-store';

/**
 * Bloque l'accès direct par URL à un module hors des permissions du rôle (UX seulement :
 * le backend refuse de toute façon avec 403, §10.4). Renvoie vers l'accueil du rôle.
 */
export function permissionGuard(permission: string): CanActivateFn {
  return () => inject(AuthStore).hasPermission(permission) || inject(Router).createUrlTree(['/']);
}
