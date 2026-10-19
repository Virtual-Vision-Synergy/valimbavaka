import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth-guard';
import { permissionGuard } from './core/auth/permission-guard';
import { homeGuard } from './core/layout/home-guard';
import { MainLayout } from './core/layout/main-layout/main-layout';
import { PLACEHOLDER_ROUTES } from './features/module-placeholder/module-placeholder.routes';

/** Une feature = un fichier de routes chargé à la demande (convention §2.4). */
export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: '',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', canActivate: [homeGuard], children: [] },
      {
        path: 'users',
        canActivate: [permissionGuard('users.read')],
        loadChildren: () => import('./features/user/user.routes').then((m) => m.USER_ROUTES),
      },
      {
        path: 'journal',
        canActivate: [permissionGuard('journal.read')],
        loadChildren: () =>
          import('./features/journal/journal.routes').then((m) => m.JOURNAL_ROUTES),
      },
      ...PLACEHOLDER_ROUTES,
    ],
  },
  { path: '**', redirectTo: '' },
];
