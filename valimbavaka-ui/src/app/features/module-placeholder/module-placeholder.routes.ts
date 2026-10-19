import { Route, Routes } from '@angular/router';
import { permissionGuard } from '../../core/auth/permission-guard';
import { MENU_SECTIONS } from '../../core/layout/menu';

const NO_ACCESS_MESSAGE =
  "Aucun module n'est accessible avec le rôle de votre compte. Contactez l'administrateur.";

/** Routes provisoires des modules du menu (hors utilisateurs), chacune protégée par sa permission. */
export const PLACEHOLDER_ROUTES: Routes = [
  ...MENU_SECTIONS.flatMap((section) => section.items)
    .filter((item) => item.route !== '/users')
    .map(
      (item): Route => ({
        path: item.route.slice(1),
        title: `${item.label} · Valimbavaka`,
        canActivate: [permissionGuard(item.permission)],
        data: { heading: item.label },
        loadComponent: () => import('./module-placeholder').then((m) => m.ModulePlaceholder),
      }),
    ),
  {
    path: 'no-access',
    title: 'Aucun accès · Valimbavaka',
    data: { heading: 'Aucun accès', message: NO_ACCESS_MESSAGE },
    loadComponent: () => import('./module-placeholder').then((m) => m.ModulePlaceholder),
  },
];
