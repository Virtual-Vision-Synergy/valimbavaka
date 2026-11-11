import { Routes } from '@angular/router';

/** Ventes du jour (VTE-07, API) ; « en construction » tant que FAC-02 à FAC-06 (impression, avoir, livre) manquent. */
export const SALES_ROUTES: Routes = [
  {
    path: '',
    title: 'sales.title',
    data: { underConstruction: true },
    loadComponent: () => import('./pages/sales-page/sales-page').then((m) => m.SalesPage),
  },
];
