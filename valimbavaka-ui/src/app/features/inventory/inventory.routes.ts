import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/auth/permission-guard';

/**
 * Inventaire. `new` : ouverture d'une session (INV-01) ; la page de comptage lit la session en cours, enregistre
 * les quantités (INV-02) et les motifs d'écart (INV-03), puis clôture la session (INV-04) ; `history` : toutes les
 * sessions avec leur statut et leur nombre d'écarts (INV-05). Plus rien n'est en démonstration.
 */
export const INVENTORY_ROUTES: Routes = [
  {
    path: '',
    title: 'inventory.pageTitle',
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
  {
    path: 'history',
    title: 'inventory.history.title',
    loadComponent: () =>
      import('./pages/inventory-history-page/inventory-history-page').then(
        (m) => m.InventoryHistoryPage,
      ),
  },
];
