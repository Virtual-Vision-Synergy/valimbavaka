import { SaleChannel } from '../../../shared/models/commerce';

/** Statut affiché sur le tableau de suivi (une colonne par statut ; les commandes annulées n'y figurent pas). */
export type DeliveryStatus = 'TO_PREPARE' | 'ON_ROUND' | 'DELIVERED' | 'FAILED';

/** Miroir de l'énumération backend DeliveryStatus (LIV-01) : statuts du tableau + annulée. */
export type DeliveryOrderStatus = DeliveryStatus | 'CANCELLED';

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

/** Commande à livrer telle qu'affichée sur le tableau de suivi (vue construite par `delivery-mapping.ts`). */
export interface DeliveryRDto {
  readonly id: string;
  readonly number: string;
  readonly time: string;
  readonly customerName: string;
  readonly district: string;
  readonly address: string;
  readonly phone: string;
  /** Canal de prise de commande ; null pour une commande lue sur l'API (le serveur ne le connaît pas). */
  readonly channel: SaleChannel | null;
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

/** Miroir de DeliveryOrderLineRDto : prix unitaire figé à la création, `amount` = quantité × prix. */
export interface DeliveryOrderLineRDto {
  readonly articleId: string;
  readonly quantity: number;
  readonly unitPrice: number;
  readonly amount: number;
}

/** Miroir de DeliveryOrderRDto (LIV-01) : la tournée d'un livreur = `driverId` + `scheduledDate`. */
export interface DeliveryOrderRDto {
  readonly id: string;
  readonly number: string;
  readonly status: DeliveryOrderStatus;
  readonly customerName: string;
  readonly customerPhone: string;
  readonly address: string;
  readonly district: string | null;
  readonly driverId: string;
  readonly scheduledDate: string;
  readonly stopNumber: number;
  readonly amount: number;
  readonly note: string | null;
  readonly lines: readonly DeliveryOrderLineRDto[];
  readonly createdAt: string;
  readonly createdBy: string | null;
  readonly version: number;
}

/** Miroir de DeliveryOrderLineWDto : le prix est lu par le serveur sur la fiche article. */
export interface DeliveryOrderLineWDto {
  readonly articleId: string;
  readonly quantity: number;
}

/** Miroir de DeliveryOrderWDto : ni numéro, ni statut, ni prix (attribués par le serveur). */
export interface DeliveryOrderWDto {
  readonly customerName: string;
  readonly customerPhone: string;
  readonly address: string;
  readonly district: string | null;
  readonly driverId: string;
  readonly scheduledDate: string;
  readonly lines: readonly DeliveryOrderLineWDto[];
  readonly note: string | null;
}

/** Miroir de DeliveryDriverRDto : livreur actif proposé à l'affectation. */
export interface DeliveryDriverRDto {
  readonly id: string;
  readonly firstName: string;
  readonly lastName: string;
}
