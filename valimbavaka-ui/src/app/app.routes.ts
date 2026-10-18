import { Routes } from '@angular/router';
import { MainLayout } from './core/layout/main-layout/main-layout';

/** Une feature = un fichier de routes chargé à la demande (convention §2.4). */
export const routes: Routes = [
  {
    path: '',
    component: MainLayout,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'users' },
      {
        path: 'users',
        loadChildren: () => import('./features/user/user.routes').then((m) => m.USER_ROUTES),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
