import { Routes } from '@angular/router';

/** Production : création, validation, statuts et indicateurs (PRD-01 à PRD-04) ; détail et coût d'un lot (PRD-05). */
export const PRODUCTION_ROUTES: Routes = [
  {
    path: '',
    title: 'production.title',
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/production-page/production-page').then((m) => m.ProductionPage),
  },
  {
    path: ':id',
    title: 'production.detail.pageTitle',
    loadComponent: () =>
      import('./pages/production-lot-page/production-lot-page').then((m) => m.ProductionLotPage),
  },
];
