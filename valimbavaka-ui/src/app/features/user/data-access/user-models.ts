/** Rôles métier (SYS-05), valeurs identiques au JSON du backend. */
export type RoleName = 'ADMIN' | 'SELLER' | 'DELIVERY_DRIVER';

export interface RoleRDto {
  id: string;
  name: RoleName;
  /** Libellé affiché (Administrateur, Vendeuse, Livreur). */
  description: string | null;
}

export interface UserRDto {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: RoleRDto;
  active: boolean;
  passwordChangeRequired: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserWDto {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  roleId: string;
}

export interface UpdateUserWDto {
  email: string;
  firstName: string;
  lastName: string;
  roleId: string;
  active: boolean;
}

export function roleLabel(role: RoleRDto): string {
  return role.description ?? role.name;
}
