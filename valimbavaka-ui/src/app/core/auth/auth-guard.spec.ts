import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';
import { authGuard, guestGuard } from './auth-guard';
import { AuthService } from './auth-service';

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

  it('guestGuard should send an authenticated user back to the home page', () => {
    setup(true);
    let result: boolean | UrlTree | undefined;

    run(guestGuard).subscribe((value) => (result = value));

    expect(urlOf(result as UrlTree)).toBe('/');
  });
});
