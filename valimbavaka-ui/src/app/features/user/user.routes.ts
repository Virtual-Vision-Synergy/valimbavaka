import { Routes } from '@angular/router';

export const USER_ROUTES: Routes = [
  {
    path: '',
    title: 'Utilisateurs · Valimbavaka',
    loadComponent: () =>
      import('./pages/user-list-page/user-list-page').then((m) => m.UserListPage),
  },
  {
    path: 'new',
    title: 'Nouvel utilisateur · Valimbavaka',
    loadComponent: () =>
      import('./pages/user-form-page/user-form-page').then((m) => m.UserFormPage),
  },
  {
    path: ':id/edit',
    title: 'Modifier un utilisateur · Valimbavaka',
    loadComponent: () =>
      import('./pages/user-form-page/user-form-page').then((m) => m.UserFormPage),
  },
];
