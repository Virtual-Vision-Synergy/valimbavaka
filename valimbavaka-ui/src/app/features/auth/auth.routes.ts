import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  {
    path: '',
    title: 'auth.login.title',
    loadComponent: () => import('./pages/login-page/login-page').then((m) => m.LoginPage),
  },
];
