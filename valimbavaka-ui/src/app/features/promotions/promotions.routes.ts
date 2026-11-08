import { Routes } from '@angular/router';

/**
 * Promotions : liste, fiche et création branchées sur l'API (PRO-01, PRO-04). Reste « en construction » :
 * nombres d'articles du périmètre en démonstration, remise accordée et application en caisse à venir (PRO-02, PRO-03).
 */
export const PROMOTIONS_ROUTES: Routes = [
  {
    path: '',
    title: 'promotions.title',
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/promotion-list-page/promotion-list-page').then((m) => m.PromotionListPage),
  },
];
