import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { AuthStore } from '../../../core/auth/auth-store';
import { OutboxService } from '../../../core/offline/outbox-service';
import { PosContextRDto, SaleFormRDto, SaleWDto } from './pos-models';
import { PosService } from './pos-service';

const FORM: SaleFormRDto = {
  nextNumber: 7,
  channels: [{ id: 'channel-store', code: 'STORE', label: 'Boutique', stockLocationId: 'store' }],
  sellers: [],
  articles: [],
};

describe('PosService', () => {
  const submit = vi.fn();

  beforeEach(() => {
    submit.mockReset().mockReturnValue(of({ queued: true }));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: OutboxService, useValue: { submit } },
        { provide: AuthStore, useValue: { currentUser: () => null } },
      ],
    });
  });

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('lit le formulaire de vente et les gammes pour préparer le poste', () => {
    let context: PosContextRDto | undefined;

    TestBed.inject(PosService)
      .context()
      .subscribe((value) => (context = value));
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/v1/sales/form').flush(FORM);
    http.expectOne((request) => request.url === '/api/v1/product-ranges').flush({ items: [] });

    expect(context?.ticketNumber).toBe(7);
    expect(context?.channelIds).toEqual({ SHOP: 'channel-store' });
  });

  it('enregistre la vente par la file hors ligne (POST /api/v1/sales)', () => {
    const body: SaleWDto = {
      salesChannelId: 'channel-store',
      sellerId: 'seller-miora',
      paymentMethod: 'CASH',
      lines: [{ articleId: 'soap', quantity: 2 }],
    };

    TestBed.inject(PosService).record(body).subscribe();

    expect(submit).toHaveBeenCalledWith('POST', '/api/v1/sales', body);
  });
});
