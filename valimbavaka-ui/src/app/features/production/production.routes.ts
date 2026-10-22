import { Routes } from '@angular/router';

/** Production : données de démonstration tant que le module backend n'existe pas. */
export const PRODUCTION_ROUTES: Routes = [
  {
    path: '',
    title: 'production.title',
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/production-page/production-page').then((m) => m.ProductionPage),
  },
];
