import { OutboxEntry } from '../../../core/offline/outbox-models';
import { withQueuedActions } from './delivery-queue';
import { DeliveryRDto, DeliveryOrderStatus } from './delivery-models';

function stop(id: string, status: DeliveryOrderStatus = 'TO_PREPARE'): DeliveryRDto {
  return { id, status, failureReason: null } as DeliveryRDto;
}

function entry(url: string, body: unknown = null, method: OutboxEntry['method'] = 'POST') {
  return {
    id: `entry-${url}`,
    method,
    url,
    body,
    createdAt: 1,
    attempts: 0,
    status: 'PENDING',
  } satisfies OutboxEntry;
}

describe('withQueuedActions', () => {
  it('garde les arrêts tels quels quand rien ne attend le réseau', () => {
    const stops = [stop('a'), stop('b')];

    expect(withQueuedActions(stops, [])).toEqual(stops);
  });

  it('montre livrée une commande dont le marquage attend le réseau, sans toucher aux autres', () => {
    const result = withQueuedActions(
      [stop('a'), stop('b')],
      [entry('/api/v1/deliveries/a/deliver')],
    );

    expect(result.map((s) => s.status)).toEqual(['DELIVERED', 'TO_PREPARE']);
  });

  it('montre en échec avec son motif une commande dont l’échec attend le réseau', () => {
    const result = withQueuedActions(
      [stop('a')],
      [entry('/api/v1/deliveries/a/failure', { reason: 'REFUSED', note: null })],
    );

    expect(result[0]).toMatchObject({ status: 'FAILED', failureReason: 'REFUSED' });
  });

  it('retombe sur « autre » quand le motif en file est inconnu', () => {
    const result = withQueuedActions(
      [stop('a')],
      [entry('/api/v1/deliveries/a/failure', { reason: 'BIZARRE' })],
    );

    expect(result[0].failureReason).toBe('OTHER');
  });

  it('rend le statut du serveur quand une annulation suit le marquage en file', () => {
    const result = withQueuedActions(
      [stop('a', 'ON_ROUND')],
      [entry('/api/v1/deliveries/a/deliver'), entry('/api/v1/deliveries/a/undo')],
    );

    expect(result[0].status).toBe('ON_ROUND');
  });

  it('ignore les saisies qui ne sont pas des actions de tournée', () => {
    const result = withQueuedActions(
      [stop('a')],
      [
        entry('/api/v1/deliveries'),
        entry('/api/v1/sales/a/deliver'),
        entry('/api/v1/deliveries/a/deliver', null, 'PUT'),
      ],
    );

    expect(result[0].status).toBe('TO_PREPARE');
  });
});
