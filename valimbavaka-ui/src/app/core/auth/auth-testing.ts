import { CurrentUserRDto } from './auth-models';

/** Permissions de chaque rôle, miroir du seed Liquibase (2026_10_19-01) : à n'utiliser que dans les tests. */
export const ROLE_PERMISSIONS: Record<string, readonly string[]> = {
  ADMIN: [
    'dashboard.read',
    'suppliers.read',
    'suppliers.create',
    'suppliers.update',
    'articles.read',
    'stocks.read',
    'production.read',
    'transfers.read',
    'inventory.read',
    'sales.read',
    'deliveries.read',
    'promotions.read',
    'commissions.read',
    'services.read',
    'reports.read',
    'referentials.read',
    'roles.read',
    'users.read',
    'users.create',
    'users.update',
    'journal.read',
  ],
  SELLER: [
    'dashboard.read',
    'articles.read',
    'stocks.read',
    'sales.read',
    'promotions.read',
    'commissions.read',
    'services.read',
    'referentials.read',
  ],
  DELIVERY_DRIVER: ['dashboard.read', 'deliveries.read'],
};

const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Administrateur',
  SELLER: 'Vendeuse',
  DELIVERY_DRIVER: 'Livreur',
};

export function buildUser(role: keyof typeof ROLE_PERMISSIONS): CurrentUserRDto {
  return {
    id: '0195f0a0-0000-7000-8000-000000000001',
    email: `${role.toLowerCase()}@valimbavaka.mg`,
    firstName: 'Hanta',
    lastName: 'Rasolofoson',
    role,
    roleLabel: ROLE_LABELS[role],
    permissions: ROLE_PERMISSIONS[role],
    passwordChangeRequired: false,
  };
}
