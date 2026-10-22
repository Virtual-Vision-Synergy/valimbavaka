import { Routes } from '@angular/router';

/** Fournisseurs : données de démonstration tant que le module backend n'existe pas. */
export const SUPPLIERS_ROUTES: Routes = [
  {
    path: '',
    title: 'suppliers.title',
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/supplier-list-page/supplier-list-page').then((m) => m.SupplierListPage),
  },
];
