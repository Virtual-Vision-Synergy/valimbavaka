import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/auth/permission-guard';

/**
 * Inventaire. `new` : ouverture d'une session (INV-01) ; la page de comptage lit la session en cours et enregistre
 * les quantités (INV-02) ; motifs (INV-03) et clôture (INV-04) restent en démonstration.
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
