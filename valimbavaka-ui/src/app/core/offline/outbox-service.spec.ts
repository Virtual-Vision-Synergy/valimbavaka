import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { errorInterceptor } from '../http/error-interceptor';
import { ConnectivityService } from './connectivity-service';
import { OutboxService } from './outbox-service';
import { SubmitResult } from './outbox-models';

describe('OutboxService', () => {
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
    sut = TestBed.inject(OutboxService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  const submit = (url: string): Promise<SubmitResult> =>
    new Promise((resolve, reject) =>
      sut.submit('POST', url, { name: url }).subscribe({ next: resolve, error: reject }),
    );

  const answer = async (url: string, status: number): Promise<void> => {
    const request = await vi.waitFor(() => httpMock.expectOne(url));
    request.flush({ status, detail: 'refus' }, { status, statusText: 'x' });
  };

  it('should send the request with an idempotency key when online', async () => {
    const result = submit('/api/v1/items');
    const request = httpMock.expectOne('/api/v1/items');
    request.flush({});

    expect(await result).toEqual({ queued: false });
    expect(request.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
    expect(sut.pendingCount()).toBe(0);
  });

  it('should queue the request without sending it when offline', async () => {
    online.set(false);

    const result = await submit('/api/v1/items');

    expect(result).toEqual({ queued: true });
    expect(sut.pendingCount()).toBe(1);
    httpMock.expectNone('/api/v1/items');
  });

  it('should queue the request when the network fails while sending', async () => {
    const result = submit('/api/v1/items');
    httpMock.expectOne('/api/v1/items').error(new ProgressEvent('error'));

    expect(await result).toEqual({ queued: true });
    expect(sut.pendingCount()).toBe(1);
  });

  it('should not queue a business error and rethrow it', async () => {
    const result = submit('/api/v1/items');
    httpMock.expectOne('/api/v1/items').flush({ status: 422 }, { status: 422, statusText: 'x' });

    await expect(result).rejects.toMatchObject({ status: 422 });
    expect(sut.pendingCount()).toBe(0);
  });

  it('should replay queued requests in order once back online', async () => {
    online.set(false);
    await submit('/api/v1/first');
    await submit('/api/v1/second');
    online.set(true);

    const flushing = sut.flush();
    await answer('/api/v1/first', 200);
    await answer('/api/v1/second', 200);
    await flushing;

    expect(sut.pendingCount()).toBe(0);
    expect(sut.failedCount()).toBe(0);
  });

  it('should stop replaying and keep the rest when the network drops again', async () => {
    online.set(false);
    await submit('/api/v1/first');
    await submit('/api/v1/second');
    online.set(true);

    const flushing = sut.flush();
    const request = await vi.waitFor(() => httpMock.expectOne('/api/v1/first'));
    request.error(new ProgressEvent('error'));
    await flushing;

    httpMock.expectNone('/api/v1/second');
    expect(sut.pendingCount()).toBe(2);
  });

  it('should mark a refused request as failed without blocking the next one', async () => {
    online.set(false);
    await submit('/api/v1/first');
    await submit('/api/v1/second');
    online.set(true);

    const flushing = sut.flush();
    await answer('/api/v1/first', 422);
    await answer('/api/v1/second', 200);
    await flushing;

    expect(sut.failedCount()).toBe(1);
    expect(sut.failed()[0]).toMatchObject({ url: '/api/v1/first', attempts: 1 });
    expect(sut.pendingCount()).toBe(0);
  });

  it('should queue a new request behind older pending ones to keep the order', async () => {
    online.set(false);
    await submit('/api/v1/first');
    online.set(true);

    const result = submit('/api/v1/second');
    await answer('/api/v1/first', 200);
    await answer('/api/v1/second', 200);

    expect(await result).toEqual({ queued: true });
  });

  it('should resend a failed request on retry and drop it on discard', async () => {
    online.set(false);
    await submit('/api/v1/first');
    await submit('/api/v1/second');
    online.set(true);
    const flushing = sut.flush();
    await answer('/api/v1/first', 422);
    await answer('/api/v1/second', 422);
    await flushing;
    const [first, second] = sut.failed();

    const retrying = sut.retry(first.id);
    await answer('/api/v1/first', 200);
    await retrying;
    await sut.discard(second.id);

    expect(sut.failedCount()).toBe(0);
    expect(sut.pendingCount()).toBe(0);
  });

  it('should empty the queue on clear', async () => {
    online.set(false);
    await submit('/api/v1/first');

    await sut.clear();

    expect(sut.pendingCount()).toBe(0);
  });
});
