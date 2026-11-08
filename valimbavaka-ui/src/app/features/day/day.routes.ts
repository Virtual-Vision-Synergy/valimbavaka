import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/auth/permission-guard';

/**
 * Journée de vente : ouverture (répartition du matin, JR-01, `days.open`) et clôture (réconciliation,
 * verrouillage, encore en démonstration). Les permissions sont posées page par page.
 */
export const DAY_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'open' },
  {
    path: 'open',
    title: 'menu.item.dayOpen',
    canActivate: [permissionGuard('days.open')],
    loadComponent: () => import('./pages/day-open-page/day-open-page').then((m) => m.DayOpenPage),
  },
  {
    path: 'close',
    title: 'menu.item.dayClose',
    canActivate: [permissionGuard('sales.read')],
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/day-close-page/day-close-page').then((m) => m.DayClosePage),
  },
];
