import { StockLocationRDto } from '../../referential/data-access/referential-models';

/** Points de vente alimentés chaque matin depuis l'entrepôt (sous-ensemble de StockLocation). */
export type DispatchDestination = 'SHOP' | 'DELIVERY' | 'SALON';

export const DISPATCH_DESTINATIONS: readonly DispatchDestination[] = ['SHOP', 'DELIVERY', 'SALON'];

/** Code stable de l'emplacement (référentiel SYS-05) → colonne de la grille. */
export const DESTINATION_BY_LOCATION_CODE: Readonly<Record<string, DispatchDestination>> = {
  STORE: 'SHOP',
  DELIVERY: 'DELIVERY',
  TRADE_SHOW: 'SALON',
};

export type DispatchQuantities = Readonly<Record<DispatchDestination, number>>;

export const NO_QUANTITIES: DispatchQuantities = { SHOP: 0, DELIVERY: 0, SALON: 0 };

/** État d'une journée : ouverte par la répartition du matin (JR-01), close par le verrouillage (JR-06). */
export type DayStatus = 'OPEN' | 'CLOSED';

/** Transfert du matin d'une destination (serveur, JR-01). */
export interface DayTransferRDto {
  readonly transferId: string;
  readonly transferNumber: string;
  readonly locationId: string;
  readonly lineCount: number;
  readonly quantity: number;
}

/** Journée de vente (serveur) : `createdAt` = heure d'ouverture, `createdBy` = qui l'a ouverte. */
export interface BusinessDayRDto {
  readonly id: string;
  readonly businessDate: string;
  readonly status: DayStatus;
  readonly transfers: readonly DayTransferRDto[];
  readonly createdAt: string;
  readonly createdBy: string | null;
}

/** Article fini actif disponible à l'entrepôt (serveur). */
export interface DayOpeningLineRDto {
  readonly articleId: string;
  readonly code: string;
  readonly name: string;
  readonly unit: string;
  readonly threshold: number;
  readonly warehouse: number;
}

/** Grille de répartition du jour (`GET /api/v1/days/opening`) ; `day` est posé si la journée est déjà ouverte. */
export interface DayOpeningRDto {
  readonly businessDate: string;
  readonly previousDay: string | null;
  readonly day: BusinessDayRDto | null;
  readonly warehouseLocationId: string;
  readonly destinations: readonly StockLocationRDto[];
  readonly lines: readonly DayOpeningLineRDto[];
}

/** Quantité d'un article envoyée de l'entrepôt vers une destination. */
export interface DispatchLineWDto {
  readonly articleId: string;
  readonly destinationId: string;
  readonly quantity: number;
}

/** Ouverture du jour (`POST /api/v1/days`) : la date est celle du serveur ; aucune ligne = sans transfert. */
export interface DayOpeningWDto {
  readonly lines: readonly DispatchLineWDto[];
}

/** Reste de la veille réintégré automatiquement (non fourni par le serveur pour l'instant). */
export interface CarryOverRDto {
  readonly destination: DispatchDestination;
  readonly quantity: number;
}

/** Ligne de la grille : quantités proposées (zéro), modifiables ; `yesterday` sera fourni par JR-02. */
export interface DispatchLineRDto {
  readonly articleId: string;
  readonly reference: string;
  readonly name: string;
  readonly threshold: number;
  readonly warehouse: number;
  readonly carryOver: CarryOverRDto | null;
  readonly proposed: DispatchQuantities;
  readonly yesterday: DispatchQuantities;
}

/** Bon de transfert généré par destination à la validation. */
export interface DispatchVoucherRDto {
  readonly number: string;
  readonly destination: DispatchDestination;
  readonly lineCount: number;
  readonly units: number;
}

/** Grille affichée par l'écran d'ouverture, construite à partir de `DayOpeningRDto`. */
export interface DayOpeningGrid {
  readonly date: string;
  readonly previousDay: string | null;
  readonly rounds: number;
  readonly lines: readonly DispatchLineRDto[];
  /** Identifiant de l'emplacement de chaque colonne ; absent si l'emplacement est inactif. */
  readonly destinationIds: Partial<Record<DispatchDestination, string>>;
  /** Bons du matin si la journée est déjà ouverte, sinon null. */
  readonly vouchers: readonly DispatchVoucherRDto[] | null;
}

/** Motif obligatoire pour tout écart de comptage à la clôture (JR-05). */
export type DiscrepancyReason = 'BREAKAGE' | 'LOSS' | 'ENTRY_ERROR' | 'SAMPLE' | 'OTHER';

export const DISCREPANCY_REASONS: readonly DiscrepancyReason[] = [
  'BREAKAGE',
  'LOSS',
  'ENTRY_ERROR',
  'SAMPLE',
  'OTHER',
];

/** Ligne de réconciliation (JR-04) : théorique = transféré + retour − vendu − livré − salon − shooting. */
export interface ClosingLineRDto {
  readonly articleId: string;
  readonly reference: string;
  readonly name: string;
  readonly transferred: number;
  readonly returned: number;
  readonly sold: number;
  readonly delivered: number;
  readonly salon: number;
  readonly shooting: number;
  readonly counted: number;
  readonly reason: DiscrepancyReason | null;
}

export interface ClosingCashRDto {
  readonly expected: number;
  readonly counted: number;
  /** Paiements MVola / Orange Money / carte déjà rapprochés automatiquement. */
  readonly otherMethods: number;
}

export interface ClosingLocationRDto {
  readonly location: DispatchDestination;
  readonly lines: readonly ClosingLineRDto[];
  readonly cash: ClosingCashRDto;
}

export interface ClosingChecklistRDto {
  readonly deliveriesDone: number;
  readonly deliveriesTotal: number;
  readonly heldTickets: number;
  readonly lateShootings: number;
}

export interface DayClosingRDto {
  readonly date: string;
  readonly locations: readonly ClosingLocationRDto[];
  readonly checklist: ClosingChecklistRDto;
}

/** Verrouillage de la journée (JR-06) : comptages, motifs et espèces comptées. */
export interface DayLockWDto {
  readonly date: string;
  readonly counts: readonly {
    readonly location: DispatchDestination;
    readonly articleId: string;
    readonly counted: number;
    readonly reason: DiscrepancyReason | null;
  }[];
  readonly cash: readonly { readonly location: DispatchDestination; readonly counted: number }[];
}

export function theoretical(line: ClosingLineRDto): number {
  return line.transferred + line.returned - line.sold - line.delivered - line.salon - line.shooting;
}

export function sumQuantities(quantities: DispatchQuantities): number {
  return quantities.SHOP + quantities.DELIVERY + quantities.SALON;
}

/** Grille de l'écran à partir de la réponse du serveur : quantités proposées à zéro. */
export function toOpeningGrid(opening: DayOpeningRDto): DayOpeningGrid {
  const destinationIds: Partial<Record<DispatchDestination, string>> = {};
  for (const location of opening.destinations) {
    const destination = DESTINATION_BY_LOCATION_CODE[location.code];
    if (destination) {
      destinationIds[destination] = location.id;
    }
  }
  return {
    date: opening.businessDate,
    previousDay: opening.previousDay,
    rounds: 0,
    destinationIds,
    vouchers: opening.day ? toVouchers(opening.day, destinationIds) : null,
    lines: opening.lines.map((line) => ({
      articleId: line.articleId,
      reference: line.code,
      name: line.name,
      threshold: line.threshold,
      warehouse: line.warehouse,
      carryOver: null,
      proposed: NO_QUANTITIES,
      yesterday: NO_QUANTITIES,
    })),
  };
}

/** Bons du matin d'une journée ouverte, dans l'ordre du serveur ; un emplacement hors grille est ignoré. */
export function toVouchers(
  day: BusinessDayRDto,
  destinationIds: DayOpeningGrid['destinationIds'],
): DispatchVoucherRDto[] {
  return day.transfers.flatMap((transfer) => {
    const destination = DISPATCH_DESTINATIONS.find(
      (key) => destinationIds[key] === transfer.locationId,
    );
    return destination
      ? [
          {
            number: transfer.transferNumber,
            destination,
            lineCount: transfer.lineCount,
            units: transfer.quantity,
          },
        ]
      : [];
  });
}

/** Corps de l'ouverture : une ligne par article et par destination dont la quantité est positive. */
export function toDayOpeningWDto(
  grid: DayOpeningGrid,
  rows: readonly { readonly articleId: string; readonly quantities: DispatchQuantities }[],
): DayOpeningWDto {
  const lines: DispatchLineWDto[] = [];
  for (const row of rows) {
    for (const destination of DISPATCH_DESTINATIONS) {
      const destinationId = grid.destinationIds[destination];
      const quantity = row.quantities[destination];
      if (destinationId && quantity > 0) {
        lines.push({ articleId: row.articleId, destinationId, quantity });
      }
    }
  }
  return { lines };
}
