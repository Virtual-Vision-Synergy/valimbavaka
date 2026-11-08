import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { AuthStore } from '../../../core/auth/auth-store';
import { buildUser } from '../../../core/auth/auth-testing';
import { OutboxService } from '../../../core/offline/outbox-service';
import {
  DeliveryBoardRDto,
  DeliveryEventRDto,
  DeliveryOrderWDto,
  DriverRoundRDto,
} from './delivery-models';
import { DeliveryService } from './delivery-service';

const EMPTY_PAGE = { items: [], page: 1, pageSize: 100, totalCount: 0, totalPages: 0 };

describe('DeliveryService', () => {
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

  it('envoie la commande par la file hors ligne (POST /api/v1/deliveries)', () => {
    const body: DeliveryOrderWDto = {
      customerName: 'Rakoto Be',
      customerPhone: '034 12 345 67',
      address: 'Lot II J 45, rue Pasteur',
      district: null,
      driverId: 'tojo',
      scheduledDate: '2026-11-09',
      lines: [{ articleId: 'soap', quantity: 2 }],
      note: null,
    };

    TestBed.inject(DeliveryService).create(body).subscribe();

    expect(submit).toHaveBeenCalledWith('POST', '/api/v1/deliveries', body);
  });

  it('lit les commandes du jour dans l’ordre des arrêts et les livreurs pour le tableau', () => {
    let board: DeliveryBoardRDto | undefined;
    TestBed.inject(DeliveryService)
      .board('2026-11-09')
      .subscribe((value) => (board = value));
    const http = TestBed.inject(HttpTestingController);

    const orders = http.expectOne((req) => req.url === '/api/v1/deliveries');
    expect(orders.request.params.get('date')).toBe('2026-11-09');
    expect(orders.request.params.get('size')).toBe('100');
    expect(orders.request.params.get('sortBy')).toBe('stopNumber');
    expect(orders.request.params.get('sortDirection')).toBe('ASC');
    orders.flush(EMPTY_PAGE);
    http.expectOne((req) => req.url === '/api/v1/deliveries/drivers').flush(EMPTY_PAGE);

    expect(board).toEqual({ rounds: [], deliveries: [] });
  });

  it('lit l’historique d’une commande, une page de 100 étapes au plus', () => {
    const step: DeliveryEventRDto = {
      occurredAt: '2026-11-08T06:15:00Z',
      actorName: 'Hanta Rabe',
      action: 'CREATE',
      description: 'Création de la commande à livrer LIV-0001',
    };
    let steps: DeliveryEventRDto[] | undefined;
    TestBed.inject(DeliveryService)
      .history('id-1')
      .subscribe((value) => (steps = value));

    const request = TestBed.inject(HttpTestingController).expectOne(
      (req) => req.url === '/api/v1/deliveries/id-1/history',
    );
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('100');
    request.flush({ ...EMPTY_PAGE, items: [step], totalCount: 1, totalPages: 1 });

    expect(steps).toEqual([step]);
  });

  it('lit la tournée du jour du livreur connecté, arrêt par arrêt', () => {
    const driver = buildUser('DELIVERY_DRIVER');
    TestBed.inject(AuthStore).signIn('jeton', driver);
    let round: DriverRoundRDto | undefined;
    TestBed.inject(DeliveryService)
      .myRound('2026-11-09')
      .subscribe((value) => (round = value));
    const http = TestBed.inject(HttpTestingController);

    const orders = http.expectOne((req) => req.url === '/api/v1/deliveries');
    expect(orders.request.params.get('driverId')).toBe(driver.id);
    expect(orders.request.params.get('date')).toBe('2026-11-09');
    expect(orders.request.params.get('size')).toBe('100');
    expect(orders.request.params.get('sortBy')).toBe('stopNumber');
    expect(orders.request.params.get('sortDirection')).toBe('ASC');
    orders.flush(EMPTY_PAGE);

    expect(round).toEqual({
      round: { id: driver.id, name: 'Hanta R.', driverName: 'Hanta R.' },
      date: '2026-11-09',
      stops: [],
    });
  });
});
