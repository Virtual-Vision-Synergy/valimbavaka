import { Routes } from '@angular/router';

/** Promotions : données de démonstration tant que le module backend n'existe pas. */
export const PROMOTIONS_ROUTES: Routes = [
  {
    path: '',
    title: 'promotions.title',
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/promotion-list-page/promotion-list-page').then((m) => m.PromotionListPage),
  },
];
