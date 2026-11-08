import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { OutboxService } from '../../../core/offline/outbox-service';
import { StockReturnWDto } from './transfer-models';
import { TransferService } from './transfer-service';

describe('TransferService', () => {
  const submit = vi.fn();

  beforeEach(() => {
    submit.mockReset().mockReturnValue(of({ queued: true }));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: OutboxService, useValue: { submit } },
      ],
    });
  });

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('envoie le retour de stock par la file hors ligne (POST /api/v1/transfers/returns)', () => {
    const body: StockReturnWDto = {
      fromLocationId: 'store-id',
      toLocationId: 'warehouse-id',
      lines: [{ articleId: 'article-id', quantity: 2.5 }],
      note: null,
    };
    let queued: boolean | undefined;

    TestBed.inject(TransferService)
      .recordReturn(body)
      .subscribe((result) => (queued = result.queued));

    expect(submit).toHaveBeenCalledWith('POST', '/api/v1/transfers/returns', body);
    expect(queued).toBe(true);
  });

  it("lit l'historique paginé sur GET /api/v1/transfers, filtré par origine", () => {
    TestBed.inject(TransferService).list({ page: 2, size: 9, origin: 'MORNING' }).subscribe();

    const request = TestBed.inject(HttpTestingController).expectOne(
      (req) => req.url === '/api/v1/transfers',
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('9');
    expect(request.request.params.get('origin')).toBe('MORNING');
    request.flush({ items: [], page: 2, pageSize: 9, totalCount: 0, totalPages: 0 });
  });

  it("n'envoie pas de filtre d'origine quand toutes les origines sont demandées", () => {
    TestBed.inject(TransferService).list({ page: 1, size: 9 }).subscribe();

    const request = TestBed.inject(HttpTestingController).expectOne(
      (req) => req.url === '/api/v1/transfers',
    );
    expect(request.request.params.has('origin')).toBe(false);
    request.flush({ items: [], page: 1, pageSize: 9, totalCount: 0, totalPages: 0 });
  });

  it("lit le détail d'un transfert sur GET /api/v1/transfers/{id}", () => {
    let number: string | undefined;

    TestBed.inject(TransferService)
      .get('0195f0a0-0000-7000-8000-0000000000c1')
      .subscribe((transfer) => (number = transfer.number));

    const request = TestBed.inject(HttpTestingController).expectOne(
      '/api/v1/transfers/0195f0a0-0000-7000-8000-0000000000c1',
    );
    expect(request.request.method).toBe('GET');
    request.flush({ number: 'RET-0015' });
    expect(number).toBe('RET-0015');
  });
});
