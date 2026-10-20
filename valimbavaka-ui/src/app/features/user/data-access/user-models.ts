/** Rôles métier (SYS-05), valeurs identiques au JSON du backend. */
export type RoleName = 'ADMIN' | 'SELLER' | 'DELIVERY_DRIVER';

export interface RoleRDto {
  id: string;
  name: RoleName;
  /** Libellé affiché (Administrateur, Vendeuse, Livreur). */
  description: string | null;
}

export interface SalesChannelRDto {
  id: string;
  code: string;
  label: string;
}

export interface UserRDto {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: RoleRDto;
  salesChannelId: string | null;
  /** Pourcentage (0 à 100) ; renseigné uniquement pour une vendeuse. */
  commissionRate: number | null;
  active: boolean;
  passwordChangeRequired: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UserStatusWDto {
  active: boolean;
}

export interface CreateUserWDto {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  roleId: string;
  salesChannelId: string | null;
  commissionRate: number | null;
}

export interface UpdateUserWDto {
  email: string;
  firstName: string;
  lastName: string;
  roleId: string;
  salesChannelId: string | null;
  commissionRate: number | null;
  active: boolean;
}

export function roleLabel(role: RoleRDto): string {
  return role.description ?? role.name;
}
