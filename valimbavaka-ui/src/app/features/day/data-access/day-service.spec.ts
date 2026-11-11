import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { OutboxService } from '../../../core/offline/outbox-service';
import {
  DayClosing,
  DayClosingRDto,
  DayOpeningGrid,
  DayOpeningRDto,
  theoretical,
  toDayOpeningWDto,
} from './day-models';
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
      yesterday: [],
    },
  ],
};

const CLOSING: DayClosingRDto = {
  businessDate: '2026-11-11',
  status: 'OPEN',
  locations: [
    {
      locationId: 'store-id',
      code: 'STORE',
      label: 'Boutique',
      lines: [
        {
          articleId: 'soap',
          reference: 'ART-0031',
          name: 'Savon noir',
          opening: 4,
          transferred: 30,
          returned: 1,
          sold: 19,
          delivered: 0,
          salon: 0,
          shooting: 2,
          theoretical: 14,
          systemStock: 12,
          gap: -2,
        },
      ],
    },
    { locationId: 'delivery-id', code: 'DELIVERY', label: 'Livraison', lines: [] },
    { locationId: 'warehouse-id', code: 'WAREHOUSE', label: 'Entrepôt', lines: [] },
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

  it('range les quantités du dernier jour ouvré par colonne (JR-02)', () => {
    let grid: DayOpeningGrid | undefined;
    const yesterday = [
      { locationId: 'store-id', quantity: 12 },
      { locationId: 'salon-id', quantity: 3 },
      { locationId: 'unknown-id', quantity: 9 },
    ];

    TestBed.inject(DayService)
      .opening()
      .subscribe((value) => (grid = value));
    TestBed.inject(HttpTestingController)
      .expectOne('/api/v1/days/opening')
      .flush({ ...OPENING, lines: [{ ...OPENING.lines[0], yesterday }] });

    expect(grid?.previousDay).toBe('2026-11-07');
    expect(grid?.lines[0].yesterday).toEqual({ SHOP: 12, DELIVERY: 0, SALON: 3 });
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

  it('lit la réconciliation sur GET /api/v1/days/closing et la range par colonne (JR-04)', () => {
    let closing: DayClosing | undefined;

    TestBed.inject(DayService)
      .closing()
      .subscribe((value) => (closing = value));
    TestBed.inject(HttpTestingController).expectOne('/api/v1/days/closing').flush(CLOSING);

    expect(closing?.date).toBe('2026-11-11');
    // un emplacement hors des trois colonnes (l'entrepôt) est ignoré
    expect(closing?.locations.map((block) => block.location)).toEqual(['SHOP', 'DELIVERY']);
    const line = closing?.locations[0].lines[0];
    expect(line).toMatchObject({ reference: 'ART-0031', opening: 4, transferred: 30, shooting: 2 });
    // le comptage proposé est le stock du système, sans motif
    expect(line).toMatchObject({ counted: 12, reason: null });
  });

  it('calcule le théorique : report + transféré + retour − vendu − livré − salon − shooting', () => {
    const line = {
      articleId: 'soap',
      reference: 'ART-0031',
      name: 'Savon noir',
      opening: 4,
      transferred: 30,
      returned: 1,
      sold: 19,
      delivered: 2,
      salon: 3,
      shooting: 1,
      counted: 0,
      reason: null,
    };

    expect(theoretical(line)).toBe(10);
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
