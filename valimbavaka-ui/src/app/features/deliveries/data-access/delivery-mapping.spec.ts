import {
  businessToday,
  driverLabel,
  eventTime,
  isPendingStop,
  toBoard,
  toDriverRound,
} from './delivery-mapping';
import { DeliveryDriverRDto, DeliveryOrderRDto, DeliveryOrderStatus } from './delivery-models';

const TOJO: DeliveryDriverRDto = { id: 'tojo', firstName: 'Tojo', lastName: 'Nirina' };
const ANDO: DeliveryDriverRDto = { id: 'ando', firstName: 'Ando', lastName: 'Rabe' };

function order(
  number: string,
  driverId: string,
  status: DeliveryOrderStatus = 'TO_PREPARE',
): DeliveryOrderRDto {
  return {
    id: `id-${number}`,
    number,
    status,
    customerName: 'Rakoto Be',
    customerPhone: '034 12 345 67',
    address: 'Lot II J 45, rue Pasteur',
    district: 'Ivandry',
    driverId,
    scheduledDate: '2026-11-09',
    stopNumber: 2,
    amount: 17000,
    note: null,
    failureReason: null,
    failureNote: null,
    returnTransferId: null,
    lines: [{ articleId: 'soap', quantity: 2, unitPrice: 8500, amount: 17000 }],
    createdAt: '2026-11-09T05:32:00Z',
    createdBy: 'seller',
    saleId: null,
    version: 0,
  };
}

describe('delivery-mapping', () => {
  it('donne le jour d’Antananarivo, déjà le lendemain à 22:30 UTC', () => {
    expect(businessToday(new Date('2026-11-08T22:30:00Z'))).toBe('2026-11-09');
    expect(businessToday(new Date('2026-11-08T20:59:00Z'))).toBe('2026-11-08');
  });

  it('nomme le livreur par son prénom et l’initiale de son nom', () => {
    expect(driverLabel(TOJO)).toBe('Tojo N.');
    expect(driverLabel({ id: 'x', firstName: 'Lova', lastName: ' ' })).toBe('Lova');
  });

  it('fait une carte par commande, rangée dans la tournée de son livreur à son arrêt', () => {
    const board = toBoard([order('LIV-0001', 'tojo')], [ANDO, TOJO]);

    expect(board.rounds).toEqual([{ id: 'tojo', name: 'Tojo N.', driverName: 'Tojo N.' }]);
    expect(board.deliveries[0]).toMatchObject({
      number: 'LIV-0001',
      status: 'TO_PREPARE',
      roundId: 'tojo',
      stop: 2,
      amount: 17000,
      articleCount: 1,
      phone: '034 12 345 67',
      address: 'Ivandry, Lot II J 45, rue Pasteur',
      channel: null,
      time: '08:32',
    });
  });

  it('garde les commandes annulées (filtre « Annulée ») et la tournée d’un livreur désactivé depuis', () => {
    const board = toBoard(
      [order('LIV-0001', 'tojo', 'CANCELLED'), order('LIV-0002', 'gone', 'DELIVERED')],
      [TOJO],
    );

    expect(board.deliveries.map((delivery) => [delivery.number, delivery.status])).toEqual([
      ['LIV-0001', 'CANCELLED'],
      ['LIV-0002', 'DELIVERED'],
    ]);
    expect(board.rounds).toEqual([
      { id: 'tojo', name: 'Tojo N.', driverName: 'Tojo N.' },
      { id: 'gone', name: '—', driverName: '—' },
    ]);
  });

  it('date une étape de l’historique au jour et à l’heure d’Antananarivo', () => {
    expect(eventTime('2026-11-08T06:15:00Z')).toBe('08/11 09:15');
    expect(eventTime('2026-11-30T21:30:00Z')).toBe('01/12 00:30');
  });

  it('range la tournée du livreur par arrêt, avec client, adresse, téléphone et montant à encaisser', () => {
    const third = { ...order('LIV-0003', 'tojo', 'ON_ROUND'), stopNumber: 3 };
    const first = { ...order('LIV-0001', 'tojo'), stopNumber: 1, district: null, amount: 8500 };
    const cancelled = { ...order('LIV-0002', 'tojo', 'CANCELLED'), stopNumber: 2 };

    const round = toDriverRound([third, cancelled, first], TOJO, '2026-11-09');

    expect(round.round).toEqual({ id: 'tojo', name: 'Tojo N.', driverName: 'Tojo N.' });
    expect(round.date).toBe('2026-11-09');
    expect(round.stops.map((stop) => [stop.number, stop.stop])).toEqual([
      ['LIV-0001', 1],
      ['LIV-0003', 3],
    ]);
    expect(round.stops[0]).toMatchObject({
      customerName: 'Rakoto Be',
      address: 'Lot II J 45, rue Pasteur',
      phone: '034 12 345 67',
      amount: 8500,
      prepaid: false,
    });
    expect(round.stops[1].address).toBe('Ivandry, Lot II J 45, rue Pasteur');
  });

  it('montre le motif d’une commande en échec sur sa carte', () => {
    const failed = { ...order('LIV-0003', 'tojo', 'FAILED'), failureReason: 'REFUSED' as const };

    const board = toBoard([failed], [TOJO]);

    expect(board.deliveries[0]).toMatchObject({ status: 'FAILED', failureReason: 'REFUSED' });
  });

  it('donne une tournée vide quand le livreur n’a aucun arrêt ce jour-là', () => {
    expect(toDriverRound([], TOJO, '2026-11-09').stops).toEqual([]);
  });

  it('compte comme à faire les arrêts à préparer ou en tournée, pas les livrés ni les échecs', () => {
    expect(isPendingStop('TO_PREPARE')).toBe(true);
    expect(isPendingStop('ON_ROUND')).toBe(true);
    expect(isPendingStop('DELIVERED')).toBe(false);
    expect(isPendingStop('FAILED')).toBe(false);
  });
});
