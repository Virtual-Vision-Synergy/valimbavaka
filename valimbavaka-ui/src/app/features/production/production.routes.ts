import { Routes } from '@angular/router';

/** Production : création d'un lot branchée (PRD-01) ; validation, statuts et indicateurs à venir (PRD-02 à PRD-05). */
export const PRODUCTION_ROUTES: Routes = [
  {
    path: '',
    title: 'production.title',
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/production-page/production-page').then((m) => m.ProductionPage),
  },
];
