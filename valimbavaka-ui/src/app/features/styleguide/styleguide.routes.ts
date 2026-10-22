import { Routes } from '@angular/router';

/** Guide de style : textes de démonstration en dur assumés (outil interne, jamais servi en production). */
export const STYLEGUIDE_ROUTES: Routes = [
  {
    path: '',
    title: 'Guide de style',
    loadComponent: () =>
      import('./pages/styleguide-page/styleguide-page').then((m) => m.StyleguidePage),
  },
  {
    // Aperçu visuel du cockpit hors coquille (sans session) : données de démonstration uniquement.
    path: 'dashboard',
    title: 'dashboard.title',
    data: { underConstruction: true },
    loadComponent: () =>
      import('../dashboard/pages/dashboard-page/dashboard-page').then((m) => m.DashboardPage),
  },
];
