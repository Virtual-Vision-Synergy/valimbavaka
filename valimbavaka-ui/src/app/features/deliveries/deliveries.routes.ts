import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/auth/permission-guard';

/**
 * Suivi des livraisons : le tableau du jour lit l'API (LIV-01) ; détail, historique et actions restent des
 * données de démonstration jusqu'à LIV-06 et LIV-07. La création est réservée à `deliveries.create`.
 */
export const DELIVERIES_ROUTES: Routes = [
  {
    path: '',
    title: 'deliveries.title',
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/deliveries-page/deliveries-page').then((m) => m.DeliveriesPage),
  },
  {
    path: 'new',
    title: 'deliveries.form.title',
    canActivate: [permissionGuard('deliveries.create')],
    loadComponent: () =>
      import('./pages/delivery-order-page/delivery-order-page').then((m) => m.DeliveryOrderPage),
  },
];
