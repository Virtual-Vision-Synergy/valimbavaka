import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { OutboxService } from '../../../core/offline/outbox-service';
import { DayOpeningGrid, DayOpeningRDto, toDayOpeningWDto } from './day-models';
import { DayService } from './day-service';

const OPENING: DayOpeningRDto = {
  businessDate: '2026-11-08',
  previousDay: '2026-11-07',
  day: null,
  warehouseLocationId: 'warehouse-id',
  destinations: [
    { id: 'store-id', code: 'STORE', label: 'Boutique' },
    { id: 'delivery-id', code: 'DELIVERY', label: 'Livraison' },
    { id: 'salon-id', code: 'TRADE_SHOW', label: 'Salon' },
  ],
  lines: [
    {
      articleId: 'soap',
      code: 'ART-0031',
      name: 'Savon noir',
      unit: 'PIECE',
      threshold: 40,
      warehouse: 20,
    },
  ],
};

describe('DayService', () => {
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

  it('lit la grille du jour sur GET /api/v1/days/opening et la prépare pour l’écran', () => {
    let grid: DayOpeningGrid | undefined;

    TestBed.inject(DayService)
      .opening()
      .subscribe((value) => (grid = value));
    TestBed.inject(HttpTestingController).expectOne('/api/v1/days/opening').flush(OPENING);

    expect(grid?.date).toBe('2026-11-08');
    expect(grid?.destinationIds).toEqual({
      SHOP: 'store-id',
      DELIVERY: 'delivery-id',
      SALON: 'salon-id',
    });
    expect(grid?.vouchers).toBeNull();
    expect(grid?.lines[0]).toMatchObject({
      reference: 'ART-0031',
      warehouse: 20,
      proposed: { SHOP: 0, DELIVERY: 0, SALON: 0 },
    });
  });

  it('reprend les bons du matin quand la journée est déjà ouverte', () => {
    let grid: DayOpeningGrid | undefined;
    const day = {
      id: 'day-id',
      businessDate: '2026-11-08',
      status: 'OPEN' as const,
      transfers: [
        {
          transferId: 't1',
          transferNumber: 'TRF-0093',
          locationId: 'delivery-id',
          lineCount: 2,
          quantity: 7,
        },
      ],
      createdAt: '2026-11-08T04:30:00Z',
      createdBy: null,
    };

    TestBed.inject(DayService)
      .opening()
      .subscribe((value) => (grid = value));
    TestBed.inject(HttpTestingController)
      .expectOne('/api/v1/days/opening')
      .flush({ ...OPENING, day });

    expect(grid?.vouchers).toEqual([
      { number: 'TRF-0093', destination: 'DELIVERY', lineCount: 2, units: 7 },
    ]);
  });

  it('ouvre la journée par la file hors ligne (POST /api/v1/days)', () => {
    const body = { lines: [{ articleId: 'soap', destinationId: 'store-id', quantity: 4 }] };
    let queued: boolean | undefined;

    TestBed.inject(DayService)
      .open(body)
      .subscribe((result) => (queued = result.queued));

    expect(submit).toHaveBeenCalledWith('POST', '/api/v1/days', body);
    expect(queued).toBe(true);
  });

  it('n’envoie que les quantités positives, une ligne par article et par destination', () => {
    const grid: DayOpeningGrid = {
      date: '2026-11-08',
      previousDay: null,
      rounds: 0,
      lines: [],
      destinationIds: { SHOP: 'store-id', DELIVERY: 'delivery-id' },
      vouchers: null,
    };

    const body = toDayOpeningWDto(grid, [
      { articleId: 'soap', quantities: { SHOP: 12, DELIVERY: 0, SALON: 3 } },
      { articleId: 'oil', quantities: { SHOP: 0, DELIVERY: 5, SALON: 0 } },
    ]);

    expect(body.lines).toEqual([
      { articleId: 'soap', destinationId: 'store-id', quantity: 12 },
      { articleId: 'oil', destinationId: 'delivery-id', quantity: 5 },
    ]);
  });
});
