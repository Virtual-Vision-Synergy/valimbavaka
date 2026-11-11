import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/auth/permission-guard';

/**
 * Ventes par canal (VTE-08, administrateur) et ventes du jour (VTE-07, API) ; « en construction » tant que FAC-02 à
 * FAC-06 (impression, avoir, livre des ventes) manquent.
 */
export const SALES_ROUTES: Routes = [
  {
    path: 'channels',
    title: 'sales.dashboard.title',
    canActivate: [permissionGuard('reports.read')],
    loadComponent: () =>
      import('./pages/channel-dashboard-page/channel-dashboard-page').then(
        (m) => m.ChannelDashboardPage,
      ),
  },
  {
    path: '',
    title: 'sales.title',
    data: { underConstruction: true },
    loadComponent: () => import('./pages/sales-page/sales-page').then((m) => m.SalesPage),
  },
];
