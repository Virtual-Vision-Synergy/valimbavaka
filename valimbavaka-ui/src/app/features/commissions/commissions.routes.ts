import { Routes } from '@angular/router';

/** Mes commissions : données de démonstration tant que le module backend n'existe pas. */
export const COMMISSIONS_ROUTES: Routes = [
  {
    path: '',
    title: 'commissions.title',
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/commissions-page/commissions-page').then((m) => m.CommissionsPage),
  },
];
