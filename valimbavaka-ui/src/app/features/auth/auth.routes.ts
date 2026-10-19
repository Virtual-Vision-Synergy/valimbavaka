import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  {
    path: '',
    title: 'Connexion · Valimbavaka',
    loadComponent: () => import('./pages/login-page/login-page').then((m) => m.LoginPage),
  },
];
