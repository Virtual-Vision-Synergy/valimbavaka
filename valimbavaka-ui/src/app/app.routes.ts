import { isDevMode } from '@angular/core';
import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth-guard';
import { permissionGuard } from './core/auth/permission-guard';
import { DriverLayout } from './core/layout/driver-layout/driver-layout';
import { homeGuard } from './core/layout/home-guard';
import { MainLayout } from './core/layout/main-layout/main-layout';
import { PLACEHOLDER_ROUTES } from './features/module-placeholder/module-placeholder.routes';

/** Une feature = un fichier de routes chargé à la demande (convention §2.4). */
export const routes: Routes = [
  {
    // Guide de style vivant : servi uniquement en développement (ng serve), jamais en production.
    path: 'styleguide',
    canMatch: [() => isDevMode()],
    loadChildren: () =>
      import('./features/styleguide/styleguide.routes').then((m) => m.STYLEGUIDE_ROUTES),
  },
  {
    // Aperçu des écrans dans la coquille avec un profil de démonstration : développement uniquement.
    path: 'preview',
    canMatch: [() => isDevMode()],
    loadChildren: () =>
      import('./features/styleguide/preview.routes').then((m) => m.PREVIEW_ROUTES),
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    // Espace mobile du livreur : coquille dédiée, sans barre latérale.
    path: 'round',
    component: DriverLayout,
    canActivate: [authGuard, permissionGuard('deliveries.read')],
    loadChildren: () => import('./features/deliveries/driver.routes').then((m) => m.DRIVER_ROUTES),
  },
  {
    path: '',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', canActivate: [homeGuard], children: [] },
      {
        path: 'dashboard',
        canActivate: [permissionGuard('dashboard.read')],
        loadChildren: () =>
          import('./features/dashboard/dashboard.routes').then((m) => m.DASHBOARD_ROUTES),
      },
      {
        path: 'pos',
        canActivate: [permissionGuard('sales.read')],
        loadChildren: () => import('./features/pos/pos.routes').then((m) => m.POS_ROUTES),
      },
      {
        path: 'sales',
        canActivate: [permissionGuard('sales.read')],
        loadChildren: () => import('./features/sales/sales.routes').then((m) => m.SALES_ROUTES),
      },
      {
        path: 'articles',
        canActivate: [permissionGuard('articles.read')],
        loadChildren: () =>
          import('./features/articles/articles.routes').then((m) => m.ARTICLES_ROUTES),
      },
      {
        path: 'stocks',
        canActivate: [permissionGuard('stocks.read')],
        loadChildren: () => import('./features/stocks/stocks.routes').then((m) => m.STOCKS_ROUTES),
      },
      {
        path: 'deliveries',
        canActivate: [permissionGuard('deliveries.read')],
        loadChildren: () =>
          import('./features/deliveries/deliveries.routes').then((m) => m.DELIVERIES_ROUTES),
      },
      {
        path: 'day',
        canActivate: [permissionGuard('sales.read')],
        loadChildren: () => import('./features/day/day.routes').then((m) => m.DAY_ROUTES),
      },
      {
        path: 'suppliers',
        canActivate: [permissionGuard('suppliers.read')],
        loadChildren: () =>
          import('./features/suppliers/suppliers.routes').then((m) => m.SUPPLIERS_ROUTES),
      },
      {
        path: 'production',
        canActivate: [permissionGuard('production.read')],
        loadChildren: () =>
          import('./features/production/production.routes').then((m) => m.PRODUCTION_ROUTES),
      },
      {
        path: 'transfers',
        canActivate: [permissionGuard('transfers.read')],
        loadChildren: () =>
          import('./features/transfers/transfers.routes').then((m) => m.TRANSFERS_ROUTES),
      },
      {
        path: 'inventory',
        canActivate: [permissionGuard('inventory.read')],
        loadChildren: () =>
          import('./features/inventory/inventory.routes').then((m) => m.INVENTORY_ROUTES),
      },
      {
        path: 'promotions',
        canActivate: [permissionGuard('promotions.read')],
        loadChildren: () =>
          import('./features/promotions/promotions.routes').then((m) => m.PROMOTIONS_ROUTES),
      },
      {
        path: 'commissions',
        canActivate: [permissionGuard('commissions.read')],
        loadChildren: () => import('./features/commissions/commissions.routes').then((m) => m.COMMISSIONS_ROUTES),
      },
      {
        path: 'services',
        canActivate: [permissionGuard('services.read')],
        loadChildren: () => import('./features/services/services.routes').then((m) => m.SERVICES_ROUTES),
      },
      {
        path: 'reports',
        canActivate: [permissionGuard('reports.read')],
        loadChildren: () => import('./features/reports/reports.routes').then((m) => m.REPORTS_ROUTES),
      },
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
      {
        path: 'synchronisation',
        loadChildren: () => import('./features/sync/sync.routes').then((m) => m.SYNC_ROUTES),
      },
      ...PLACEHOLDER_ROUTES,
    ],
  },
  { path: '**', redirectTo: '' },
];
