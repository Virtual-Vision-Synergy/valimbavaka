import { DELIVERY_ROWS, DeliveryRow } from './delivery-fixture-rows';
import {
  DeliveryBoardRDto,
  DeliveryEventRDto,
  DeliveryRDto,
  DeliveryRoundRDto,
  DriverRoundRDto,
} from './delivery-models';

const ROUNDS: readonly DeliveryRoundRDto[] = [
  { id: 'north', name: 'Nord', driverName: 'Tojo N.' },
  { id: 'south', name: 'Sud', driverName: 'Ando R.' },
];

const NOTES: Readonly<Partial<Record<number, string>>> = {
  315: 'Sonné 2 fois, téléphone sur répondeur. Colis rapporté.',
  312: 'Aucun repère trouvé dans la zone, client injoignable.',
};

/** Historique plausible déduit du statut final (le plus récent en premier). */
function historyOf(row: DeliveryRow, driver: string): DeliveryEventRDto[] {
  const [number, time, , , , channel, , , , status] = row;
  const events: DeliveryEventRDto[] = [
    {
      kind: 'CREATED',
      when: 'Hier 17:32',
      actor: `Lova R. · ${channel === 'PHONE' ? 'Téléphone' : 'Facebook'}`,
      note: null,
    },
  ];
  if (status === 'TO_PREPARE') {
    return events;
  }
  events.unshift({ kind: 'PREPARED', when: '07:50', actor: 'Hanta R.', note: null });
  events.unshift({ kind: 'ON_ROUND', when: '08:05', actor: driver, note: null });
  if (status === 'DELIVERED') {
    events.unshift({ kind: 'DELIVERED', when: time, actor: driver, note: null });
  }
  if (status === 'FAILED') {
    events.unshift({ kind: 'ARRIVED', when: time.replace(/\d$/, '0'), actor: driver, note: null });
    events.unshift({ kind: 'FAILED', when: time, actor: driver, note: NOTES[number] ?? null });
  }
  return events;
}

function toDelivery(row: DeliveryRow): DeliveryRDto {
  const [number, time, customerName, district, address, channel, articleCount, amount, prepaid] =
    row;
  const [, , , , , , , , , status, roundId, stop, failureReason] = row;
  const driver = ROUNDS.find((round) => round.id === roundId)?.driverName ?? '—';
  return {
    id: `liv-${number}`,
    number: `LIV-0${number}`,
    time,
    customerName,
    district,
    address: `${district}, ${address}`,
    phone: `034 0${(number % 9) + 1} ${String(number * 7).slice(0, 3)} ${String(number).slice(1)}`,
    channel,
    articleCount,
    amount,
    prepaid,
    status,
    roundId,
    stop,
    failureReason,
    history: historyOf(row, driver),
  };
}

const DELIVERIES: readonly DeliveryRDto[] = DELIVERY_ROWS.map(toDelivery);

export const DELIVERY_BOARD_FIXTURE: DeliveryBoardRDto = { rounds: ROUNDS, deliveries: DELIVERIES };

/** Tournée Nord de Tojo N. (maquette LIV-02) : arrêts triés dans l'ordre de passage. */
export const DRIVER_ROUND_FIXTURE: DriverRoundRDto = {
  round: ROUNDS[0],
  date: '2026-10-21',
  stops: DELIVERIES.filter((delivery) => delivery.roundId === 'north').sort(
    (a, b) => (a.stop ?? 0) - (b.stop ?? 0),
  ),
};
