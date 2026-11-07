import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/auth/permission-guard';

/**
 * Inventaire. `new` : ouverture d'une session (INV-01) ; la page de comptage lit la session en cours, enregistre
 * les quantités (INV-02) et les motifs d'écart (INV-03), puis clôture la session (INV-04). Le bandeau « en
 * construction » reste tant que l'historique des sessions (INV-05) n'est pas branché.
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
