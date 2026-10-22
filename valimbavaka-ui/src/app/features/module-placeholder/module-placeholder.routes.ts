import { Route, Routes } from '@angular/router';
import { permissionGuard } from '../../core/auth/permission-guard';
import { MENU_SECTIONS } from '../../core/layout/menu';

/** Modules déjà livrés : ils ont leur propre route, pas de page provisoire. */
const DELIVERED_ROUTES = [
  '/users',
  '/journal',
  '/dashboard',
  '/pos',
  '/sales',
  '/articles',
  '/stocks',
  '/deliveries',
  '/day/open',
  '/day/close',
  '/suppliers',
  '/production',
  '/transfers',
  '/inventory',
  '/promotions',
  '/commissions',
  '/services',
  '/reports',
];

/** Routes provisoires des modules du menu (hors utilisateurs), chacune protégée par sa permission. */
export const PLACEHOLDER_ROUTES: Routes = [
  ...MENU_SECTIONS.flatMap((section) => section.items)
    .filter((item) => !DELIVERED_ROUTES.includes(item.route))
    .map((item): Route => ({
      path: item.route.slice(1),
      title: item.labelKey,
      canActivate: [permissionGuard(item.permission)],
      data: { headingKey: item.labelKey, underConstruction: true },
      loadComponent: () => import('./module-placeholder').then((m) => m.ModulePlaceholder),
    })),
  {
    path: 'no-access',
    title: 'placeholder.noAccess.title',
    data: {
      headingKey: 'placeholder.noAccess.title',
      messageKey: 'placeholder.noAccess.message',
    },
    loadComponent: () => import('./module-placeholder').then((m) => m.ModulePlaceholder),
  },
];
