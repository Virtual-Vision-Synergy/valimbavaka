import { Routes } from '@angular/router';
import { AuthStore } from '../../core/auth/auth-store';
import { buildUser } from '../../core/auth/auth-testing';
import { DriverLayout } from '../../core/layout/driver-layout/driver-layout';
import { MainLayout } from '../../core/layout/main-layout/main-layout';

/** Profil de démonstration propre à l'aperçu ; la session réelle (store racine) n'est pas touchée. */
function previewAuthStore(): AuthStore {
  const store = new AuthStore();
  store.signIn('preview-without-server-access', buildUser('ADMIN'));
  return store;
}

/**
 * Aperçu des écrans dans la coquille, sans connexion (développement uniquement) : le store
 * d'authentification fourni ici ne vaut que pour ce sous-arbre ; le serveur reste seul juge.
 */
export const PREVIEW_ROUTES: Routes = [
  {
    path: '',
    component: MainLayout,
    providers: [{ provide: AuthStore, useFactory: previewAuthStore }],
    children: [
      {
        path: 'dashboard',
        loadChildren: () => import('../dashboard/dashboard.routes').then((m) => m.DASHBOARD_ROUTES),
      },
      { path: 'pos', loadChildren: () => import('../pos/pos.routes').then((m) => m.POS_ROUTES) },
      {
        path: 'sales',
        loadChildren: () => import('../sales/sales.routes').then((m) => m.SALES_ROUTES),
      },
      {
        path: 'articles',
        loadChildren: () => import('../articles/articles.routes').then((m) => m.ARTICLES_ROUTES),
      },
      {
        path: 'stocks',
        loadChildren: () => import('../stocks/stocks.routes').then((m) => m.STOCKS_ROUTES),
      },
      {
        path: 'deliveries',
        loadChildren: () =>
          import('../deliveries/deliveries.routes').then((m) => m.DELIVERIES_ROUTES),
      },
      {
        path: 'day',
        loadChildren: () => import('../day/day.routes').then((m) => m.DAY_ROUTES),
      },
      {
        path: 'suppliers',
        loadChildren: () => import('../suppliers/suppliers.routes').then((m) => m.SUPPLIERS_ROUTES),
      },
      {
        path: 'production',
        loadChildren: () =>
          import('../production/production.routes').then((m) => m.PRODUCTION_ROUTES),
      },
      {
        path: 'transfers',
        loadChildren: () => import('../transfers/transfers.routes').then((m) => m.TRANSFERS_ROUTES),
      },
      {
        path: 'inventory',
        loadChildren: () => import('../inventory/inventory.routes').then((m) => m.INVENTORY_ROUTES),
      },
      {
        path: 'promotions',
        loadChildren: () =>
          import('../promotions/promotions.routes').then((m) => m.PROMOTIONS_ROUTES),
      },
      {
        path: 'commissions',
        loadChildren: () => import('../commissions/commissions.routes').then((m) => m.COMMISSIONS_ROUTES),
      },
      {
        path: 'services',
        loadChildren: () => import('../services/services.routes').then((m) => m.SERVICES_ROUTES),
      },
      {
        path: 'reports',
        loadChildren: () => import('../reports/reports.routes').then((m) => m.REPORTS_ROUTES),
      },
    ],
  },
  {
    path: 'round',
    component: DriverLayout,
    providers: [{ provide: AuthStore, useFactory: previewAuthStore }],
    loadChildren: () => import('../deliveries/driver.routes').then((m) => m.DRIVER_ROUTES),
  },
];
