import { Routes } from '@angular/router';

/** Journée de vente : ouverture (répartition) et clôture (réconciliation, verrouillage). */
export const DAY_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'open' },
  {
    path: 'open',
    title: 'menu.item.dayOpen',
    data: { underConstruction: true },
    loadComponent: () => import('./pages/day-open-page/day-open-page').then((m) => m.DayOpenPage),
  },
  {
    path: 'close',
    title: 'menu.item.dayClose',
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/day-close-page/day-close-page').then((m) => m.DayClosePage),
  },
];
