/** Utilisateur connecté tel que renvoyé par le backend : son rôle est celui du compte (AUTH-03). */
export interface CurrentUserRDto {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  /** Libellé affiché (Administrateur, Vendeuse, Livreur). */
  roleLabel: string;
  permissions: readonly string[];
  passwordChangeRequired: boolean;
}

export interface AuthRDto {
  accessToken: string;
  tokenType: 'Bearer';
  /** Durée de vie du jeton d'accès, en secondes. */
  expiresIn: number;
  user: CurrentUserRDto;
}

/** Identifiants seuls : le rôle n'est jamais envoyé, le serveur applique celui du compte. */
export interface LoginWDto {
  email: string;
  password: string;
}

/** Changement de son propre mot de passe (le provisoire doit être changé à la première connexion). */
export interface PasswordChangeWDto {
  currentPassword: string;
  newPassword: string;
}
