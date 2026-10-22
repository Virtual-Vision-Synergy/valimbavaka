import { Routes } from '@angular/router';

/** Cockpit du jour : données de démonstration tant que le module backend n'existe pas. */
export const DASHBOARD_ROUTES: Routes = [
  {
    path: '',
    title: 'dashboard.title',
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/dashboard-page/dashboard-page').then((m) => m.DashboardPage),
  },
];
