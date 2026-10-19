import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthRDto } from '../auth/auth-models';
import { AuthStore } from '../auth/auth-store';
import { buildUser } from '../auth/auth-testing';
import { authInterceptor } from './auth-interceptor';
import { ApiError } from './problem-detail';
import { errorInterceptor } from './error-interceptor';
import { sessionInterceptor } from './session-interceptor';

const UNAUTHORIZED = { status: 401, statusText: 'Unauthorized' };
const UNAUTHENTICATED_BODY = { status: 401, code: 'UNAUTHENTICATED', detail: 'Authentification requise' };

describe('sessionInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authStore: AuthStore;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor, sessionInterceptor, errorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authStore = TestBed.inject(AuthStore);
    router = TestBed.inject(Router);
    authStore.signIn('ancien-jeton', buildUser('SELLER'));
  });

  afterEach(() => httpMock.verify());

  it('should refresh once and replay the request with the new token after a 401', () => {
    const received: unknown[] = [];
    http.get('/api/v1/articles').subscribe((body) => received.push(body));

    httpMock.expectOne('/api/v1/articles').flush(UNAUTHENTICATED_BODY, UNAUTHORIZED);
    const refresh: AuthRDto = {
      accessToken: 'nouveau-jeton',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: buildUser('SELLER'),
    };
    httpMock.expectOne('/api/v1/auth/refresh').flush(refresh);
    const replay = httpMock.expectOne('/api/v1/articles');
    replay.flush({ items: [] });

    expect(replay.request.headers.get('Authorization')).toBe('Bearer nouveau-jeton');
    expect(received).toEqual([{ items: [] }]);
  });

  it('should sign out and go to the login page when the refresh fails', () => {
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    let failure: unknown;
    http.get('/api/v1/articles').subscribe({ error: (error: unknown) => (failure = error) });

    httpMock.expectOne('/api/v1/articles').flush(UNAUTHENTICATED_BODY, UNAUTHORIZED);
    httpMock.expectOne('/api/v1/auth/refresh').flush(UNAUTHENTICATED_BODY, UNAUTHORIZED);

    expect(authStore.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/login']);
    expect(failure).toBeInstanceOf(ApiError);
  });

  it('should not try to refresh after a 403', () => {
    let failure: unknown;
    http.get('/api/v1/users').subscribe({ error: (error: unknown) => (failure = error) });

    httpMock
      .expectOne('/api/v1/users')
      .flush({ status: 403, code: 'FORBIDDEN' }, { status: 403, statusText: 'Forbidden' });

    expect((failure as ApiError).status).toBe(403);
    expect(authStore.isAuthenticated()).toBe(true);
  });

  it('should not try to refresh when the login itself is rejected', () => {
    let failure: unknown;
    http.post('/api/v1/auth/login', {}).subscribe({ error: (error: unknown) => (failure = error) });

    httpMock.expectOne('/api/v1/auth/login').flush(UNAUTHENTICATED_BODY, UNAUTHORIZED);

    expect((failure as ApiError).status).toBe(401);
  });
});
