import {
  DeliveryBoardRDto,
  DeliveryDriverRDto,
  DeliveryOrderRDto,
  DeliveryRDto,
  DeliveryRoundRDto,
  DeliveryStatus,
  DriverRoundRDto,
} from './delivery-models';

/** Fuseau métier : les jours de tournée et les heures affichées sont ceux d'Antananarivo. */
export const BUSINESS_TIME_ZONE = 'Indian/Antananarivo';

/** Jour courant à Antananarivo au format ISO `yyyy-MM-dd` (jour de tournée par défaut). */
export function businessToday(now: Date = new Date()): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: BUSINESS_TIME_ZONE }).format(now);
}

/** « Tojo N. » : prénom et initiale du nom, comme sur la maquette. */
export function driverLabel(driver: DeliveryDriverRDto): string {
  const initial = driver.lastName.trim().charAt(0);
  return initial ? `${driver.firstName} ${initial}.` : driver.firstName;
}

function timeOf(instant: string): string {
  return new Intl.DateTimeFormat('fr-FR', {
    timeZone: BUSINESS_TIME_ZONE,
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(instant));
}

/** Une commande de l'API en carte du tableau ; l'historique viendra de LIV-07. */
export function toDelivery(order: DeliveryOrderRDto, status: DeliveryStatus): DeliveryRDto {
  return {
    id: order.id,
    number: order.number,
    time: timeOf(order.createdAt),
    customerName: order.customerName,
    district: order.district ?? '',
    address: order.district ? `${order.district}, ${order.address}` : order.address,
    phone: order.customerPhone,
    channel: null,
    articleCount: order.lines.length,
    amount: order.amount,
    prepaid: false,
    status,
    roundId: order.driverId,
    stop: order.stopNumber,
    failureReason: order.failureReason,
    history: [],
  };
}

/**
 * Tableau du jour : une tournée par livreur ayant au moins une commande (ordre de la liste des livreurs), les
 * commandes annulées sont écartées (aucune colonne ne les montre).
 */
export function toBoard(
  orders: readonly DeliveryOrderRDto[],
  drivers: readonly DeliveryDriverRDto[],
): DeliveryBoardRDto {
  const deliveries = orders.flatMap((order) =>
    order.status === 'CANCELLED' ? [] : [toDelivery(order, order.status)],
  );
  const driverIds = new Set(deliveries.map((delivery) => delivery.roundId));
  const rounds: DeliveryRoundRDto[] = drivers
    .filter((driver) => driverIds.has(driver.id))
    .map((driver) => ({
      id: driver.id,
      name: driverLabel(driver),
      driverName: driverLabel(driver),
    }));
  for (const id of driverIds) {
    if (id && !rounds.some((round) => round.id === id)) {
      rounds.push({ id, name: '—', driverName: '—' });
    }
  }
  return { rounds, deliveries };
}

/** Arrêt encore à faire : ni livré ni en échec (une commande à préparer peut être livrée d'un geste, LIV-01). */
export function isPendingStop(status: DeliveryStatus): boolean {
  return status === 'TO_PREPARE' || status === 'ON_ROUND';
}

/**
 * Tournée du jour d'un livreur (LIV-02) : ses commandes dans l'ordre des arrêts (`stopNumber` croissant), les
 * commandes annulées écartées.
 */
export function toDriverRound(
  orders: readonly DeliveryOrderRDto[],
  driver: DeliveryDriverRDto,
  day: string,
): DriverRoundRDto {
  const stops = orders
    .flatMap((order) => (order.status === 'CANCELLED' ? [] : [toDelivery(order, order.status)]))
    .sort((a, b) => (a.stop ?? 0) - (b.stop ?? 0));
  const label = driverLabel(driver);
  return { round: { id: driver.id, name: label, driverName: label }, date: day, stops };
}
