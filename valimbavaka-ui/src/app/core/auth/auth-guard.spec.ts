import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';
import { authGuard, guestGuard, passwordChangeGuard } from './auth-guard';
import { AuthService } from './auth-service';
import { AuthStore } from './auth-store';
import { buildUser } from './auth-testing';

describe('auth guards', () => {
  const setup = (isAuthenticated: boolean) =>
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { restoreSession: () => of(isAuthenticated) } },
      ],
    });

  const run = (guard: typeof authGuard) =>
    TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    ) as Observable<boolean | UrlTree>;

  const urlOf = (result: boolean | UrlTree) =>
    TestBed.inject(Router).serializeUrl(result as UrlTree);

  it('authGuard should redirect an anonymous visitor to the login page', () => {
    setup(false);
    let result: boolean | UrlTree | undefined;

    run(authGuard).subscribe((value) => (result = value));

    expect(urlOf(result as UrlTree)).toBe('/login');
  });

  it('authGuard should let an authenticated user through', () => {
    setup(true);
    let result: boolean | UrlTree | undefined;

    run(authGuard).subscribe((value) => (result = value));

    expect(result).toBe(true);
  });

  const signInWithTemporaryPassword = () =>
    TestBed.inject(AuthStore).signIn('jeton', {
      ...buildUser('ADMIN'),
      permissions: [],
      passwordChangeRequired: true,
    });

  it('authGuard should send a user with a temporary password to the password page', () => {
    setup(true);
    signInWithTemporaryPassword();
    let result: boolean | UrlTree | undefined;

    run(authGuard).subscribe((value) => (result = value));

    expect(urlOf(result as UrlTree)).toBe('/password');
  });

  it('passwordChangeGuard should open the password page only when the change is required', () => {
    setup(true);
    signInWithTemporaryPassword();
    let required: boolean | UrlTree | undefined;
    run(passwordChangeGuard).subscribe((value) => (required = value));

    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
    let done: boolean | UrlTree | undefined;
    run(passwordChangeGuard).subscribe((value) => (done = value));

    expect(required).toBe(true);
    expect(urlOf(done as UrlTree)).toBe('/');
  });

  it('passwordChangeGuard should send an anonymous visitor to the login page', () => {
    setup(false);
    let result: boolean | UrlTree | undefined;

    run(passwordChangeGuard).subscribe((value) => (result = value));

    expect(urlOf(result as UrlTree)).toBe('/login');
  });

  it('guestGuard should send an authenticated user back to the home page', () => {
    setup(true);
    let result: boolean | UrlTree | undefined;

    run(guestGuard).subscribe((value) => (result = value));

    expect(urlOf(result as UrlTree)).toBe('/');
  });
});
