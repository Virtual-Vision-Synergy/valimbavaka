import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthRDto, CurrentUserRDto } from '../auth/auth-models';
import { AuthStore } from '../auth/auth-store';
import { buildUser } from '../auth/auth-testing';
import { authInterceptor } from '../http/auth-interceptor';
import { errorInterceptor } from '../http/error-interceptor';
import { sessionInterceptor } from '../http/session-interceptor';
import { ConnectivityService } from './connectivity-service';
import { OutboxService } from './outbox-service';

const SUPPLIERS = '/api/v1/suppliers';
const ARTICLES = '/api/v1/articles';
const REFRESH = '/api/v1/auth/refresh';
const UNAUTHORIZED = { status: 401, statusText: 'Unauthorized' };
const UNAUTHENTICATED_BODY = { status: 401, code: 'UNAUTHENTICATED', detail: 'Session expirée' };
const OWNER = buildUser('SELLER');
const OTHER: CurrentUserRDto = {
  ...buildUser('ADMIN'),
  id: '0195f0a0-0000-7000-8000-000000000002',
};

/** SYS-11 : session expirée pendant une coupure, avec la vraie chaîne d'intercepteurs (jeton, refresh, erreurs). */
describe('OutboxService when the session expires during an outage', () => {
  const online = signal(true);
  let sut: OutboxService;
  let httpMock: HttpTestingController;
  let authStore: AuthStore;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    online.set(true);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(
          withInterceptors([authInterceptor, sessionInterceptor, errorInterceptor]),
        ),
        provideHttpClientTesting(),
        { provide: ConnectivityService, useValue: { isOnline: online.asReadonly() } },
      ],
    });
    authStore = TestBed.inject(AuthStore);
    authStore.signIn('jeton-expire', OWNER);
    sut = TestBed.inject(OutboxService);
    httpMock = TestBed.inject(HttpTestingController);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(async () => {
    await sut.clear();
    httpMock.verify();
  });

  const renewal = (accessToken: string, user: CurrentUserRDto = OWNER): AuthRDto => ({
    accessToken,
    tokenType: 'Bearer',
    expiresIn: 900,
    user,
  });

  const submit = (url: string): Promise<unknown> =>
    new Promise((resolve, reject) =>
      sut.submit('POST', url, { name: 'Huiles Ravo' }).subscribe({ next: resolve, error: reject }),
    );

  const expectRequest = (url: string, timeout = 1_000): Promise<TestRequest> =>
    vi.waitFor(() => httpMock.expectOne(url), { timeout });

  const tokenOf = (request: TestRequest): string | null =>
    request.request.headers.get('Authorization');

  const keyOf = (request: TestRequest): string | null =>
    request.request.headers.get('Idempotency-Key');

  /** Saisie faite hors ligne avec un jeton expiré entre-temps, puis retour du réseau. */
  const queueOfflineThenReconnect = async (): Promise<TestRequest> => {
    online.set(false);
    await submit(SUPPLIERS);
    online.set(true);
    TestBed.tick();
    const first = await expectRequest(SUPPLIERS);
    first.flush(UNAUTHENTICATED_BODY, UNAUTHORIZED);
    return first;
  };

  const signIn = (accessToken: string, user: CurrentUserRDto): void => {
    authStore.signIn(accessToken, user);
    TestBed.tick();
  };

  it('should renew the expired session on reconnection and send the queued entry with the new token', async () => {
    const first = await queueOfflineThenReconnect();
    expect(tokenOf(first)).toBe('Bearer jeton-expire');

    (await expectRequest(REFRESH)).flush(renewal('jeton-neuf'));
    const replay = await expectRequest(SUPPLIERS);
    replay.flush({});

    expect(tokenOf(replay)).toBe('Bearer jeton-neuf');
    expect(keyOf(replay)).toBe(keyOf(first));
    await vi.waitFor(() => expect(sut.pendingCount()).toBe(0));
    expect(navigate).not.toHaveBeenCalled();
  });

  it('should keep the entry and invite to sign in again when the refresh is refused, then send it after the same account signs in', async () => {
    const first = await queueOfflineThenReconnect();
    (await expectRequest(REFRESH)).flush(UNAUTHENTICATED_BODY, UNAUTHORIZED);

    await vi.waitFor(() =>
      expect(navigate).toHaveBeenCalledWith(['/login'], { queryParams: { session: 'expired' } }),
    );
    expect(authStore.isAuthenticated()).toBe(false);
    TestBed.tick();
    httpMock.expectNone(SUPPLIERS);

    signIn('jeton-reconnexion', OWNER);
    expect(sut.pending()).toEqual([
      expect.objectContaining({ url: SUPPLIERS, attempts: 0, status: 'PENDING' }),
    ]);
    const resent = await expectRequest(SUPPLIERS);
    resent.flush({});

    expect(tokenOf(resent)).toBe('Bearer jeton-reconnexion');
    expect(keyOf(resent)).toBe(keyOf(first));
    await vi.waitFor(() => expect(sut.pendingCount()).toBe(0));
  });

  it('should not send the expired account entries when another account signs in on the same device', async () => {
    await queueOfflineThenReconnect();
    (await expectRequest(REFRESH)).flush(UNAUTHENTICATED_BODY, UNAUTHORIZED);
    await vi.waitFor(() => expect(authStore.isAuthenticated()).toBe(false));

    signIn('jeton-b', OTHER);
    expect(sut.pendingCount()).toBe(0);
    expect(sut.failedCount()).toBe(0);
    const ownWrite = submit(ARTICLES);
    const sentByOther = await expectRequest(ARTICLES);
    sentByOther.flush({});
    await ownWrite;
    expect(tokenOf(sentByOther)).toBe('Bearer jeton-b');
    httpMock.expectNone(SUPPLIERS);

    signIn('jeton-a', OWNER);
    const sentByOwner = await expectRequest(SUPPLIERS);
    sentByOwner.flush({});
    expect(tokenOf(sentByOwner)).toBe('Bearer jeton-a');
    await vi.waitFor(() => expect(sut.pendingCount()).toBe(0));
  });

  it('should keep the session and retry later when the network drops again during the refresh', async () => {
    await queueOfflineThenReconnect();
    (await expectRequest(REFRESH)).error(new ProgressEvent('error'));

    await vi.waitFor(() =>
      expect(sut.pending()).toEqual([expect.objectContaining({ attempts: 1, status: 'PENDING' })]),
    );
    expect(authStore.isAuthenticated()).toBe(true);
    expect(navigate).not.toHaveBeenCalled();

    (await expectRequest(SUPPLIERS, 3_000)).flush(UNAUTHENTICATED_BODY, UNAUTHORIZED);
    (await expectRequest(REFRESH)).flush(renewal('jeton-neuf'));
    const replay = await expectRequest(SUPPLIERS);
    replay.flush({});

    expect(tokenOf(replay)).toBe('Bearer jeton-neuf');
    await vi.waitFor(() => expect(sut.pendingCount()).toBe(0));
  });
});
