import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AuthStore } from '../auth/auth-store';
import { buildUser } from '../auth/auth-testing';
import { errorInterceptor } from '../http/error-interceptor';
import { ConnectivityService } from './connectivity-service';
import { OutboxService } from './outbox-service';

/** Règle d'ordre en cas de blocage (SYS-10) appliquée par le moteur de la file. */
describe('OutboxService order rule when an entry is blocked', () => {
  const online = signal(true);
  let sut: OutboxService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    online.set(true);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: ConnectivityService, useValue: { isOnline: online.asReadonly() } },
      ],
    });
    TestBed.inject(AuthStore).signIn('jeton-a', buildUser('SELLER'));
    sut = TestBed.inject(OutboxService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.useRealTimers();
  });

  const submit = (url: string): Promise<unknown> =>
    new Promise((resolve, reject) =>
      sut.submit('POST', url, { name: url }).subscribe({ next: resolve, error: reject }),
    );

  const sale = '/api/v1/sales';
  const invoice = '/api/v1/invoices';
  const supplier = '/api/v1/suppliers/s-1';

  /** Met les saisies en file hors ligne puis lance le rejeu ; renvoie la promesse du rejeu, sans l'attendre. */
  const queueOfflineThenFlush = async (...urls: string[]): Promise<{ done: Promise<void> }> => {
    vi.useFakeTimers();
    online.set(false);
    for (const url of urls) {
      await submit(url);
    }
    online.set(true);
    const done = sut.flush();
    await vi.advanceTimersByTimeAsync(0);
    return { done };
  };

  const failByNetwork = async (url: string): Promise<void> => {
    httpMock.expectOne(url).error(new ProgressEvent('error'));
    await vi.advanceTimersByTimeAsync(0);
  };

  const succeed = async (url: string): Promise<void> => {
    httpMock.expectOne(url).flush({});
    await vi.advanceTimersByTimeAsync(0);
  };

  /** Échecs réseau des 4 relances (1, 2, 4, 8 s) qui suivent le premier échec : la saisie passe FAILED. */
  const failRemainingRetries = async (url: string): Promise<void> => {
    for (const delayMs of [1_000, 2_000, 4_000, 8_000]) {
      await vi.advanceTimersByTimeAsync(delayMs);
      await failByNetwork(url);
    }
  };

  it('should send an independent record ahead of an operation blocked by the network', async () => {
    const flushing = await queueOfflineThenFlush(sale, supplier);

    await failByNetwork(sale);
    await succeed(supplier);
    httpMock.expectNone(sale);
    await vi.advanceTimersByTimeAsync(1_000);
    await succeed(sale);
    await flushing.done;

    expect(sut.pendingCount()).toBe(0);
    expect(sut.failedCount()).toBe(0);
  });

  it('should keep operations in entry order behind a blocked one', async () => {
    const flushing = await queueOfflineThenFlush(sale, invoice);

    await failByNetwork(sale);
    await vi.advanceTimersByTimeAsync(999);
    httpMock.expectNone(invoice);
    await vi.advanceTimersByTimeAsync(1);
    await succeed(sale);
    await succeed(invoice);
    await flushing.done;

    expect(sut.pendingCount()).toBe(0);
  });

  it('should hold later operations behind one whose network retries are exhausted until it is resent', async () => {
    const flushing = await queueOfflineThenFlush(sale, invoice, supplier);

    await failByNetwork(sale);
    await succeed(supplier);
    await failRemainingRetries(sale);
    await flushing.done;
    httpMock.expectNone(invoice);
    expect(sut.failed()).toMatchObject([{ url: sale, status: 'FAILED', networkFailure: true }]);
    expect(sut.pending().map((entry) => entry.url)).toEqual([invoice]);

    const retrying = sut.retry(sut.failed()[0].id);
    await vi.advanceTimersByTimeAsync(0);
    await succeed(sale);
    await succeed(invoice);
    await retrying;

    expect(sut.pendingCount()).toBe(0);
    expect(sut.failedCount()).toBe(0);
  });

  it('should release the held operations when the blocked one is discarded', async () => {
    const flushing = await queueOfflineThenFlush(sale, invoice);
    await failByNetwork(sale);
    await failRemainingRetries(sale);
    await flushing.done;
    httpMock.expectNone(invoice);

    await sut.discard(sut.failed()[0].id);
    await vi.advanceTimersByTimeAsync(0);
    await succeed(invoice);

    expect(sut.pendingCount()).toBe(0);
    expect(sut.failedCount()).toBe(0);
  });

  it('should not hold operations behind one refused by the server', async () => {
    const flushing = await queueOfflineThenFlush(sale, invoice);

    httpMock.expectOne(sale).flush({ status: 422 }, { status: 422, statusText: 'x' });
    await vi.advanceTimersByTimeAsync(0);
    await succeed(invoice);
    await flushing.done;

    expect(sut.failed()).toMatchObject([{ url: sale, networkFailure: false }]);
    expect(sut.pendingCount()).toBe(0);
  });

  it('should keep the edits of one record in entry order', async () => {
    const status = `${supplier}/status`;
    const flushing = await queueOfflineThenFlush(supplier, status);

    await failByNetwork(supplier);
    httpMock.expectNone(status);
    await vi.advanceTimersByTimeAsync(1_000);
    await succeed(supplier);
    await succeed(status);
    await flushing.done;

    expect(sut.pendingCount()).toBe(0);
  });
});
