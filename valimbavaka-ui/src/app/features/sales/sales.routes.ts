import { Routes } from '@angular/router';

/** Ventes du jour (VTE-07, API) ; « en construction » tant que factures (FAC-01) et livre des ventes manquent. */
export const SALES_ROUTES: Routes = [
  {
    path: '',
    title: 'sales.title',
    data: { underConstruction: true },
    loadComponent: () => import('./pages/sales-page/sales-page').then((m) => m.SalesPage),
  },
];
