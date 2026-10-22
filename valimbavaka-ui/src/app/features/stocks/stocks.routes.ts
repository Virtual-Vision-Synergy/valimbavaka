import { Routes } from '@angular/router';

/** Stocks par emplacement : données de démonstration tant que le module backend n'existe pas. */
export const STOCKS_ROUTES: Routes = [
  {
    path: '',
    title: 'stocks.title',
    data: { underConstruction: true },
    loadComponent: () => import('./pages/stock-page/stock-page').then((m) => m.StockPage),
  },
];
