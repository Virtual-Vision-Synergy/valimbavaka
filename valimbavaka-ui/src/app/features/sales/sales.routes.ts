import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/auth/permission-guard';

/**
 * Ventes par canal (VTE-08) et statuts de facture (FAC-05), administrateur ; ventes du jour (VTE-07, API) ;
 * « en construction » tant que FAC-06 (livre des ventes) manque.
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
    path: 'statuses',
    title: 'sales.statusTotals.title',
    canActivate: [permissionGuard('reports.read')],
    loadComponent: () =>
      import('./pages/invoice-status-page/invoice-status-page').then((m) => m.InvoiceStatusPage),
  },
  {
    path: '',
    title: 'sales.title',
    data: { underConstruction: true },
    loadComponent: () => import('./pages/sales-page/sales-page').then((m) => m.SalesPage),
  },
];
