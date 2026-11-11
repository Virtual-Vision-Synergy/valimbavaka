import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { AuthStore } from '../../../core/auth/auth-store';
import { buildUser } from '../../../core/auth/auth-testing';
import { OutboxEntry } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import {
  DeliveryBoardRDto,
  DeliveryEventRDto,
  DeliveryOrderRDto,
  DeliveryOrderWDto,
  DriverRoundRDto,
} from './delivery-models';
import { DeliveryService } from './delivery-service';

const EMPTY_PAGE = { items: [], page: 1, pageSize: 100, totalCount: 0, totalPages: 0 };

function serverOrder(id: string): DeliveryOrderRDto {
  return {
    id,
    number: 'LIV-0001',
    status: 'TO_PREPARE',
    customerName: 'Rasoa',
    customerPhone: '034 12 345 61',
    address: 'Lot II J 1',
    district: null,
    driverId: 'tojo',
    scheduledDate: '2026-11-09',
    stopNumber: 1,
    amount: 8500,
    note: null,
    failureReason: null,
    failureNote: null,
    returnTransferId: null,
    lines: [],
    createdAt: '2026-11-09T05:32:00Z',
    createdBy: 'seller',
    saleId: null,
    previousStatus: null,
    updatedAt: '2026-11-09T05:32:00Z',
    version: 0,
  };
}

describe('DeliveryService', () => {
  const submit = vi.fn();
  let queued: OutboxEntry[] = [];

  beforeEach(() => {
    queued = [];
    submit.mockReset().mockReturnValue(of({ queued: true }));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: OutboxService, useValue: { submit, pending: () => queued } },
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

  it('envoie l’échec et son motif par la file hors ligne (POST /api/v1/deliveries/{id}/failure)', () => {
    let result: string | undefined;

    TestBed.inject(DeliveryService)
      .updateStatus('order-1', { status: 'FAILED', failureReason: 'ABSENT', note: 'Portail fermé' })
      .subscribe((id) => (result = id));

    expect(submit).toHaveBeenCalledWith('POST', '/api/v1/deliveries/order-1/failure', {
      reason: 'ABSENT',
      note: 'Portail fermé',
    });
    expect(result).toBe('order-1');
  });

  it('n’appelle pas l’API d’échec pour une livraison marquée livrée (LIV-03)', () => {
    TestBed.inject(DeliveryService)
      .updateStatus('order-1', { status: 'DELIVERED', failureReason: null, note: null })
      .subscribe();

    expect(submit).toHaveBeenCalledOnce();
    expect(submit).toHaveBeenCalledWith('POST', '/api/v1/deliveries/order-1/deliver');
  });

  it('annule la dernière action par la file hors ligne (POST /api/v1/deliveries/{id}/undo)', () => {
    TestBed.inject(DeliveryService).undoLastAction('order-1').subscribe();

    expect(submit).toHaveBeenCalledWith('POST', '/api/v1/deliveries/order-1/undo');
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

  it('reporte sur la tournée lue les saisies qui attendent encore le réseau (LIV-08)', () => {
    TestBed.inject(AuthStore).signIn('jeton', buildUser('DELIVERY_DRIVER'));
    queued = [
      {
        id: 'k1',
        method: 'POST',
        url: '/api/v1/deliveries/order-1/deliver',
        body: null,
        createdAt: 1,
        attempts: 0,
        status: 'PENDING',
      },
    ];
    let round: DriverRoundRDto | undefined;
    TestBed.inject(DeliveryService)
      .myRound('2026-11-09')
      .subscribe((value) => (round = value));

    TestBed.inject(HttpTestingController)
      .expectOne((req) => req.url === '/api/v1/deliveries')
      .flush({ ...EMPTY_PAGE, items: [serverOrder('order-1')], totalCount: 1, totalPages: 1 });

    expect(round?.stops.map((s) => s.status)).toEqual(['DELIVERED']);
  });

  it('marque la livraison livrée par la file hors ligne, sans corps (POST /api/v1/deliveries/{id}/deliver)', () => {
    let result: string | undefined;

    TestBed.inject(DeliveryService)
      .updateStatus('order-1', { status: 'DELIVERED', failureReason: null, note: null })
      .subscribe((id) => (result = id));

    expect(submit).toHaveBeenCalledWith('POST', '/api/v1/deliveries/order-1/deliver');
    expect(result).toBe('order-1');
  });

  it('n’appelle pas l’API « livrée » pour un échec (LIV-04)', () => {
    TestBed.inject(DeliveryService)
      .updateStatus('order-1', { status: 'FAILED', failureReason: 'ABSENT', note: null })
      .subscribe();

    expect(submit).toHaveBeenCalledOnce();
    expect(submit).toHaveBeenCalledWith('POST', '/api/v1/deliveries/order-1/failure', {
      reason: 'ABSENT',
      note: null,
    });
  });
});
