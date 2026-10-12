import { RoleRDto } from './role';

export interface UserRDto {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: RoleRDto;
  active: boolean;
  mustChangePassword: boolean;
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

export interface UserQuery {
  search: string;
  page: number;
  size: number;
}
