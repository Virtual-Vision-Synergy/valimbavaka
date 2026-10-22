import { Routes } from '@angular/router';

/** Espace mobile du livreur (LIV-02, LIV-08) : données de démonstration pour l'instant. */
export const DRIVER_ROUTES: Routes = [
  {
    path: '',
    title: 'deliveries.driver.title',
    loadComponent: () =>
      import('./pages/driver-round-page/driver-round-page').then((m) => m.DriverRoundPage),
  },
  {
    path: 'history',
    title: 'deliveries.driver.historyTitle',
    loadComponent: () =>
      import('./pages/driver-history-page/driver-history-page').then((m) => m.DriverHistoryPage),
  },
  {
    path: 'profile',
    title: 'deliveries.driver.tab.profile',
    loadComponent: () =>
      import('./pages/driver-profile-page/driver-profile-page').then((m) => m.DriverProfilePage),
  },
];
