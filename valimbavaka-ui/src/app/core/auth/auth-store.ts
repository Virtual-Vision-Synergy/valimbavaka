import { Injectable, computed, signal } from '@angular/core';
import { CurrentUserRDto } from './auth-models';

/**
 * État d'authentification en mémoire (jamais dans localStorage, convention §9.2).
 * Alimenté par AuthService (login / refresh) ; le rôle et les permissions viennent du serveur.
 */
@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly token = signal<string | null>(null);
  private readonly user = signal<CurrentUserRDto | null>(null);
  private readonly revision = signal(0);

  readonly accessToken = this.token.asReadonly();
  readonly currentUser = this.user.asReadonly();
  readonly sessionRevision = this.revision.asReadonly();
  readonly isAuthenticated = computed(() => this.token() !== null);
  private readonly grantedPermissions = computed(() => new Set(this.user()?.permissions ?? []));

  signIn(accessToken: string, user: CurrentUserRDto): void {
    this.revision.update((revision) => revision + 1);
    this.token.set(accessToken);
    this.user.set(user);
  }

  signOut(): void {
    this.revision.update((revision) => revision + 1);
    this.token.set(null);
    this.user.set(null);
  }

  hasPermission(permission: string): boolean {
    return this.grantedPermissions().has(permission);
  }
}
