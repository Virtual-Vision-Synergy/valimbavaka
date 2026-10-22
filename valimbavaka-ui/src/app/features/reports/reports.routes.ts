import { Routes } from '@angular/router';

/** Rapports : données de démonstration tant que le module backend n'existe pas. */
export const REPORTS_ROUTES: Routes = [
  {
    path: '',
    title: 'reports.title',
    data: { underConstruction: true },
    loadComponent: () => import('./pages/reports-page/reports-page').then((m) => m.ReportsPage),
  },
];
