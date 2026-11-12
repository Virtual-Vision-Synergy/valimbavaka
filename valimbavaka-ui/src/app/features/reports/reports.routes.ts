import { Routes } from '@angular/router';

/** Rapports : tout est branché sur l'API (RPT-01 à RPT-05) sauf l'export PDF/XLSX (RPT-06), d'où le marqueur. */
export const REPORTS_ROUTES: Routes = [
  {
    path: '',
    title: 'reports.title',
    data: { underConstruction: true },
    loadComponent: () => import('./pages/reports-page/reports-page').then((m) => m.ReportsPage),
  },
];
