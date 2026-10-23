import { Routes } from '@angular/router';

/** Changement du mot de passe provisoire, imposé avant tout autre écran (AUTH-01, SYS-05). */
export const PASSWORD_ROUTES: Routes = [
  {
    path: '',
    title: 'auth.password.title',
    loadComponent: () =>
      import('./pages/password-change-page/password-change-page').then((m) => m.PasswordChangePage),
  },
];
