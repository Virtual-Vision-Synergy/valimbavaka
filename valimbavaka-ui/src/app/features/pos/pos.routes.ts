import { Routes } from '@angular/router';

/** Poste de caisse : données de démonstration tant que le module de vente backend n'existe pas. */
export const POS_ROUTES: Routes = [
  {
    path: '',
    title: 'pos.title',
    data: { underConstruction: true },
    loadComponent: () => import('./pages/pos-page/pos-page').then((m) => m.PosPage),
  },
];
