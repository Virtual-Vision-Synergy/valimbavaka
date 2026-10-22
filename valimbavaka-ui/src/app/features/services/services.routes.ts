import { Routes } from '@angular/router';

/** Salon et shooting : données de démonstration tant que le module backend n'existe pas. */
export const SERVICES_ROUTES: Routes = [
  {
    path: '',
    title: 'services.title',
    data: { underConstruction: true },
    loadComponent: () => import('./pages/services-page/services-page').then((m) => m.ServicesPage),
  },
];
