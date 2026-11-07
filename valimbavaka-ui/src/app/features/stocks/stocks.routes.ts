import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/auth/permission-guard';

/**
 * Stocks par emplacement (STK-01). Réapprovisionnement : fournisseur, délai et ventes encore provisoires (STK-05).
 * Réception d'une livraison fournisseur réservée à `stocks.receive` (STK-06).
 */
export const STOCKS_ROUTES: Routes = [
  {
    path: '',
    title: 'stocks.title',
    data: { underConstruction: true },
    loadComponent: () => import('./pages/stock-page/stock-page').then((m) => m.StockPage),
  },
  {
    path: 'receipt',
    title: 'stocks.receipt.title',
    canActivate: [permissionGuard('stocks.receive')],
    loadComponent: () =>
      import('./pages/supplier-receipt-page/supplier-receipt-page').then(
        (m) => m.SupplierReceiptPage,
      ),
  },
];
