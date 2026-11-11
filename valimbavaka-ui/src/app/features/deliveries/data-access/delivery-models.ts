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

/**
 * Filtre de statut du suivi (LIV-06) : « attente / en cours / livrée / retour / annulée » du ticket, dans l'ordre
 * du tableau. `ALL` montre les quatre colonnes du tableau, sans les commandes annulées.
 */
export type DeliveryStatusFilter = DeliveryOrderStatus | 'ALL';

export const DELIVERY_STATUS_FILTERS: readonly DeliveryStatusFilter[] = [
  'ALL',
  ...DELIVERY_STATUSES,
  'CANCELLED',
];

/** Motif d'échec saisi par le livreur, miroir de DeliveryFailureReason (LIV-04). */
export type FailureReason = 'ABSENT' | 'ADDRESS_NOT_FOUND' | 'REFUSED' | 'OTHER';

/** Miroir de l'énumération backend JournalAction : nature d'une ligne du journal. */
export type JournalAction = 'CREATE' | 'UPDATE' | 'DELETE' | 'CANCEL' | 'REVERSE';

/**
 * Miroir de DeliveryEventRDto (LIV-06) : une étape de l'historique d'une commande = une ligne du journal écrite pour
 * elle (création, changement de statut, annulation), avec son instant, son auteur et son libellé.
 */
export interface DeliveryEventRDto {
  readonly occurredAt: string;
  readonly actorName: string;
  readonly action: JournalAction;
  readonly description: string;
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
  readonly status: DeliveryOrderStatus;
  readonly roundId: string | null;
  readonly stop: number | null;
  /** Jour de la tournée (`yyyy-MM-dd`, Antananarivo). */
  readonly scheduledDate: string;
  readonly note: string | null;
  readonly failureReason: FailureReason | null;
}

/** Tableau de suivi d'un jour (LIV-01, LIV-06) : commandes annulées comprises, montrées par le filtre « Annulée ». */
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

/** Miroir de DeliveryFailureWDto (LIV-04) : motif obligatoire, commentaire facultatif (280 caractères). */
export interface DeliveryFailureWDto {
  readonly reason: FailureReason;
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
  /** Motif, commentaire du livreur et retour de stock (`RET-…`) d'une commande en échec (LIV-04). */
  readonly failureReason: FailureReason | null;
  readonly failureNote: string | null;
  readonly returnTransferId: string | null;
  readonly lines: readonly DeliveryOrderLineRDto[];
  readonly createdAt: string;
  readonly createdBy: string | null;
  /** Vente (canal Livraison) créée quand la commande a été marquée livrée (LIV-03). */
  readonly saleId: string | null;
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
