import { Routes } from '@angular/router';

/** Stocks par emplacement (STK-01). Réapprovisionnement : fournisseur, délai et ventes encore provisoires (STK-05). */
export const STOCKS_ROUTES: Routes = [
  {
    path: '',
    title: 'stocks.title',
    data: { underConstruction: true },
    loadComponent: () => import('./pages/stock-page/stock-page').then((m) => m.StockPage),
  },
];
