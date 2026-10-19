import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthRDto } from './auth-models';
import { AuthService } from './auth-service';
import { AuthStore } from './auth-store';
import { buildUser } from './auth-testing';

describe('AuthService', () => {
  let sut: AuthService;
  let http: HttpTestingController;

  const response = (role: 'ADMIN' | 'SELLER'): AuthRDto => ({
    accessToken: 'jeton',
    tokenType: 'Bearer',
    expiresIn: 900,
    user: buildUser(role),
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    sut = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should store the session in memory after a successful login', () => {
    sut.login({ email: 'a@b.mg', password: 'x' }).subscribe();

    http.expectOne('/api/v1/auth/login').flush(response('SELLER'));

    expect(TestBed.inject(AuthStore).currentUser()?.role).toBe('SELLER');
  });

  it('should send a single refresh request for simultaneous callers', () => {
    sut.refresh().subscribe();
    sut.refresh().subscribe();

    http.expectOne('/api/v1/auth/refresh').flush(response('ADMIN'));
  });

  it('should report no session when the refresh cookie is rejected', () => {
    let restored: boolean | undefined;
    sut.restoreSession().subscribe((value) => (restored = value));

    http
      .expectOne('/api/v1/auth/refresh')
      .flush({ title: 'UNAUTHENTICATED' }, { status: 401, statusText: 'Unauthorized' });

    expect(restored).toBe(false);
  });

  it('should clear the session on logout even if the server call fails', () => {
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
    sut.logout().subscribe();

    http.expectOne('/api/v1/auth/logout').error(new ProgressEvent('error'));

    expect(TestBed.inject(AuthStore).isAuthenticated()).toBe(false);
  });
});
