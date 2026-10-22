import { Routes } from '@angular/router';

/** Transferts de stock : données de démonstration tant que le module backend n'existe pas. */
export const TRANSFERS_ROUTES: Routes = [
  {
    path: '',
    title: 'transfers.title',
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/transfer-list-page/transfer-list-page').then((m) => m.TransferListPage),
  },
];
