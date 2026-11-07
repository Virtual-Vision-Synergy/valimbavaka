import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/auth/permission-guard';

/**
 * Transferts de stock. La liste reste en données de démonstration jusqu'à TRF-01 ; le retour de stock (TRF-03)
 * est saisi par l'administrateur ou la vendeuse (`transfers.return`), sans accès à l'historique.
 */
export const TRANSFERS_ROUTES: Routes = [
  {
    path: '',
    title: 'transfers.title',
    canActivate: [permissionGuard('transfers.read')],
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/transfer-list-page/transfer-list-page').then((m) => m.TransferListPage),
  },
  {
    path: 'return',
    title: 'transfers.return.title',
    canActivate: [permissionGuard('transfers.return')],
    loadComponent: () =>
      import('./pages/stock-return-page/stock-return-page').then((m) => m.StockReturnPage),
  },
];
