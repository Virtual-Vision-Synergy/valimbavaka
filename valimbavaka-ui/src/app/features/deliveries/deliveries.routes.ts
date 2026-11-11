import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/auth/permission-guard';

/**
 * Suivi des livraisons : tableau d'un jour par statut, détail et historique de la commande lus sur l'API (LIV-01,
 * LIV-06). La création est réservée à `deliveries.create`.
 */
export const DELIVERIES_ROUTES: Routes = [
  {
    path: '',
    title: 'deliveries.title',
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
