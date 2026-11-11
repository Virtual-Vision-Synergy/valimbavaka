import { Routes } from '@angular/router';

/** Rapports : synthèse réelle (RPT-01) ; les autres sections restent en démonstration jusqu'à RPT-02 à RPT-05. */
export const REPORTS_ROUTES: Routes = [
  {
    path: '',
    title: 'reports.title',
    data: { underConstruction: true },
    loadComponent: () => import('./pages/reports-page/reports-page').then((m) => m.ReportsPage),
  },
];
