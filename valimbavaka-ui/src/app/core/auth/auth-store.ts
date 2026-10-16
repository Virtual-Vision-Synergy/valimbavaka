import { Injectable, computed, signal } from '@angular/core';

/**
 * État d'authentification en mémoire (jamais dans localStorage, convention §9.2).
 * Alimenté par la feature auth (login / refresh) quand elle sera livrée.
 */
@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly token = signal<string | null>(null);
  private readonly grantedPermissions = signal<ReadonlySet<string>>(new Set());

  readonly accessToken = this.token.asReadonly();
  readonly isAuthenticated = computed(() => this.token() !== null);

  signIn(accessToken: string, permissions: readonly string[]): void {
    this.token.set(accessToken);
    this.grantedPermissions.set(new Set(permissions));
  }

  signOut(): void {
    this.token.set(null);
    this.grantedPermissions.set(new Set());
  }

  hasPermission(permission: string): boolean {
    return this.grantedPermissions().has(permission);
  }
}
