import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/auth/permission-guard';

/**
 * Inventaire. `new` : ouverture d'une session branchée sur l'API (INV-01) ; la page de comptage reste en
 * démonstration jusqu'à INV-02 (comptage) et INV-04 (clôture).
 */
export const INVENTORY_ROUTES: Routes = [
  {
    path: '',
    title: 'inventory.pageTitle',
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/inventory-page/inventory-page').then((m) => m.InventoryPage),
  },
  {
    path: 'new',
    title: 'inventory.open.title',
    canActivate: [permissionGuard('inventory.create')],
    loadComponent: () =>
      import('./pages/inventory-open-page/inventory-open-page').then((m) => m.InventoryOpenPage),
  },
];
