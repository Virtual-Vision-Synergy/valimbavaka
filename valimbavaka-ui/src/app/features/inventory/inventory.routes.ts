import { Routes } from '@angular/router';

/** Inventaire : données de démonstration tant que le module backend n'existe pas. */
export const INVENTORY_ROUTES: Routes = [
  {
    path: '',
    title: 'inventory.pageTitle',
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/inventory-page/inventory-page').then((m) => m.InventoryPage),
  },
];
