import { Routes } from '@angular/router';

/** Suivi des livraisons : données de démonstration tant que le module backend n'existe pas. */
export const DELIVERIES_ROUTES: Routes = [
  {
    path: '',
    title: 'deliveries.title',
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/deliveries-page/deliveries-page').then((m) => m.DeliveriesPage),
  },
];
