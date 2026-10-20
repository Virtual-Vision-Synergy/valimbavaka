import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { AuthStore } from './auth-store';
import { buildUser } from './auth-testing';
import { permissionGuard } from './permission-guard';

describe('permissionGuard', () => {
  const run = (permission: string) =>
    TestBed.runInInjectionContext(() =>
      permissionGuard(permission)({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

  it('should allow a route when the role has the permission', () => {
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));

    expect(run('users.read')).toBe(true);
  });

  it('should redirect to the home page when the role lacks the permission', () => {
    TestBed.inject(AuthStore).signIn('jeton', buildUser('SELLER'));

    const result = run('users.read');

    expect(result instanceof UrlTree).toBe(true);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/');
  });
});
