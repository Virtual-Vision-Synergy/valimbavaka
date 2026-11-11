import { OutboxEntry } from '../../../core/offline/outbox-models';
import { DeliveryRDto, FailureReason } from './delivery-models';

const ACTION_URL = /^\/api\/v1\/deliveries\/([^/?#]+)\/(deliver|failure|undo)(?:[?#].*)?$/;
const FAILURE_REASONS: readonly FailureReason[] = [
  'ABSENT',
  'ADDRESS_NOT_FOUND',
  'REFUSED',
  'OTHER',
];

/**
 * Tournée relue pendant que des saisies attendent le réseau (LIV-08) : le serveur ne les connaît pas encore, alors
 * l'arrêt déjà marqué « livrée » ou « échec » hors ligne ne doit pas redevenir « à faire » au rechargement.
 * Les saisies sont rejouées dans l'ordre ; une annulation (LIV-05) rend l'arrêt au statut connu du serveur.
 */
export function withQueuedActions(
  stops: readonly DeliveryRDto[],
  queued: readonly OutboxEntry[],
): DeliveryRDto[] {
  return stops.map((stop) => queued.reduce((current, entry) => apply(current, stop, entry), stop));
}

function apply(current: DeliveryRDto, served: DeliveryRDto, entry: OutboxEntry): DeliveryRDto {
  const match = ACTION_URL.exec(entry.url);
  if (entry.method !== 'POST' || !match || decodeURIComponent(match[1]) !== current.id) {
    return current;
  }
  switch (match[2]) {
    case 'deliver':
      return { ...current, status: 'DELIVERED', failureReason: null };
    case 'failure':
      return { ...current, status: 'FAILED', failureReason: reasonOf(entry.body) };
    default:
      return { ...current, status: served.status, failureReason: served.failureReason };
  }
}

function reasonOf(body: unknown): FailureReason {
  const reason = (body as { reason?: FailureReason } | null)?.reason;
  return reason && FAILURE_REASONS.includes(reason) ? reason : 'OTHER';
}
