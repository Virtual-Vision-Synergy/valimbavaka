import { Routes } from '@angular/router';

/** La liste et la fiche latérale sont celles de la maquette `features/suppliers`, branchées sur l'API. */
export const SUPPLIER_ROUTES: Routes = [
  {
    path: '',
    title: 'supplier.title',
    loadComponent: () =>
      import('../suppliers/pages/supplier-list-page/supplier-list-page').then(
        (m) => m.SupplierListPage,
      ),
  },
  {
    path: 'new',
    title: 'supplier.form.newTitle',
    loadComponent: () =>
      import('./pages/supplier-form-page/supplier-form-page').then((m) => m.SupplierFormPage),
  },
  {
    path: ':id/edit',
    title: 'supplier.form.editTitle',
    loadComponent: () =>
      import('./pages/supplier-form-page/supplier-form-page').then((m) => m.SupplierFormPage),
  },
];
