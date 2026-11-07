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
      providers: [{ provide: OutboxService, useValue: { submit } }],
    });
  });

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
});
