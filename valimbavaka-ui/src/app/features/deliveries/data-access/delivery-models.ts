import { SaleChannel } from '../../../shared/models/commerce';

/** Statut d'une livraison (miroir de l'énumération backend DeliveryStatus). */
export type DeliveryStatus = 'TO_PREPARE' | 'ON_ROUND' | 'DELIVERED' | 'FAILED';

export const DELIVERY_STATUSES: readonly DeliveryStatus[] = [
  'TO_PREPARE',
  'ON_ROUND',
  'DELIVERED',
  'FAILED',
];

/** Motif d'échec saisi par le livreur (LIV-08). */
export type FailureReason = 'ABSENT' | 'ADDRESS_NOT_FOUND' | 'REFUSED' | 'OTHER';

export type DeliveryEventKind =
  'CREATED' | 'PREPARED' | 'ON_ROUND' | 'ARRIVED' | 'DELIVERED' | 'FAILED';

/** Étape de l'historique de statut (LIV-07). */
export interface DeliveryEventRDto {
  readonly kind: DeliveryEventKind;
  readonly when: string;
  readonly actor: string;
  readonly note: string | null;
}

export interface DeliveryRoundRDto {
  readonly id: string;
  readonly name: string;
  readonly driverName: string;
}

/** Commande à livrer (LIV-01). */
export interface DeliveryRDto {
  readonly id: string;
  readonly number: string;
  readonly time: string;
  readonly customerName: string;
  readonly district: string;
  readonly address: string;
  readonly phone: string;
  readonly channel: SaleChannel;
  readonly articleCount: number;
  readonly amount: number;
  /** Payée d'avance (MVola) : rien à encaisser à la porte. */
  readonly prepaid: boolean;
  readonly status: DeliveryStatus;
  readonly roundId: string | null;
  readonly stop: number | null;
  readonly failureReason: FailureReason | null;
  readonly history: readonly DeliveryEventRDto[];
}

/** Tableau de suivi du jour (LIV-01, LIV-06). */
export interface DeliveryBoardRDto {
  readonly rounds: readonly DeliveryRoundRDto[];
  readonly deliveries: readonly DeliveryRDto[];
}

/** Tournée d'un livreur (LIV-02) : ses arrêts dans l'ordre de passage. */
export interface DriverRoundRDto {
  readonly round: DeliveryRoundRDto;
  readonly date: string;
  readonly stops: readonly DeliveryRDto[];
}

/** Changement de statut saisi par le livreur (mis en file hors ligne si besoin). */
export interface DeliveryStatusWDto {
  readonly status: Extract<DeliveryStatus, 'DELIVERED' | 'FAILED'>;
  readonly failureReason: FailureReason | null;
  readonly note: string | null;
}
