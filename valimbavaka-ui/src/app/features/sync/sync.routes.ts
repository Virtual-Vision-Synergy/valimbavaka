import { Routes } from '@angular/router';

export const SYNC_ROUTES: Routes = [
  {
    path: '',
    title: 'Synchronisation · Valimbavaka',
    loadComponent: () => import('./pages/sync-page/sync-page').then((m) => m.SyncPage),
  },
];
