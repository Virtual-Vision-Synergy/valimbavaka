import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/auth/permission-guard';

/**
 * Mes commissions et objectif mensuel (COM-04) : la vendeuse voit les siens, l'administrateur ceux de la vendeuse
 * désignée par `?sellerId=` ; le serveur décide (`commissions.read`).
 * Règles de commission par canal (COM-01) et commissions calculées (COM-02) : administrateur seulement, le serveur
 * décide (permissions `commission-rules.*`, `commissions.read-all`).
 */
export const COMMISSIONS_ROUTES: Routes = [
  {
    path: '',
    title: 'commissions.title',
    loadComponent: () =>
      import('./pages/commissions-page/commissions-page').then((m) => m.CommissionsPage),
  },
  {
    path: 'calculated',
    title: 'commissionList.title',
    canActivate: [permissionGuard('commissions.read-all')],
    loadComponent: () =>
      import('./pages/commission-list-page/commission-list-page').then((m) => m.CommissionListPage),
  },
  {
    path: 'rules',
    title: 'commissionRule.title',
    canActivate: [permissionGuard('commission-rules.read')],
    loadComponent: () =>
      import('./pages/commission-rule-list-page/commission-rule-list-page').then(
        (m) => m.CommissionRuleListPage,
      ),
  },
  {
    path: 'rules/new',
    title: 'commissionRule.form.newTitle',
    canActivate: [permissionGuard('commission-rules.create')],
    loadComponent: () =>
      import('./pages/commission-rule-form-page/commission-rule-form-page').then(
        (m) => m.CommissionRuleFormPage,
      ),
  },
  {
    path: 'rules/:id/edit',
    title: 'commissionRule.form.editTitle',
    canActivate: [permissionGuard('commission-rules.update')],
    loadComponent: () =>
      import('./pages/commission-rule-form-page/commission-rule-form-page').then(
        (m) => m.CommissionRuleFormPage,
      ),
  },
];
