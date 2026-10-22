import { Routes } from '@angular/router';

/** Ventes et factures : données de démonstration tant que le module de vente backend n'existe pas. */
export const SALES_ROUTES: Routes = [
  {
    path: '',
    title: 'sales.title',
    data: { underConstruction: true },
    loadComponent: () => import('./pages/sales-page/sales-page').then((m) => m.SalesPage),
  },
];
