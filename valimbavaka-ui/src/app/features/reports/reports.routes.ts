import { Routes } from '@angular/router';

/** Rapports : tout est branché sur l'API (RPT-01 à RPT-06), plus rien n'est en démonstration. */
export const REPORTS_ROUTES: Routes = [
  {
    path: '',
    title: 'reports.title',
    loadComponent: () => import('./pages/reports-page/reports-page').then((m) => m.ReportsPage),
  },
];
