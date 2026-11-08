import { Routes } from '@angular/router';
import { permissionGuard } from '../../core/auth/permission-guard';

/**
 * Mes commissions : données de démonstration tant que le calcul (COM-02) n'existe pas.
 * Règles de commission par canal (COM-01) : administrateur seulement, le serveur décide (permissions
 * `commission-rules.*`).
 */
export const COMMISSIONS_ROUTES: Routes = [
  {
    path: '',
    title: 'commissions.title',
    data: { underConstruction: true },
    loadComponent: () =>
      import('./pages/commissions-page/commissions-page').then((m) => m.CommissionsPage),
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
