import { Routes } from '@angular/router';

export const USER_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./components/user-list/user-list').then((m) => m.UserList),
  },
  {
    path: 'new',
    loadComponent: () => import('./components/user-form/user-form').then((m) => m.UserForm),
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./components/user-form/user-form').then((m) => m.UserForm),
  },
];
