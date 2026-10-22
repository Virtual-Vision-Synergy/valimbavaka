/** Points de vente alimentés chaque matin depuis l'entrepôt (sous-ensemble de StockLocation). */
export type DispatchDestination = 'SHOP' | 'DELIVERY' | 'SALON';

export const DISPATCH_DESTINATIONS: readonly DispatchDestination[] = ['SHOP', 'DELIVERY', 'SALON'];

export type DispatchQuantities = Readonly<Record<DispatchDestination, number>>;

/** Reste de la veille réintégré automatiquement (JR-01). */
export interface CarryOverRDto {
  readonly destination: DispatchDestination;
  readonly quantity: number;
}

/** Ligne de répartition du matin (JR-02) : quantités proposées, modifiables. */
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

export interface DayOpeningRDto {
  readonly date: string;
  readonly previousDay: string;
  readonly rounds: number;
  readonly lines: readonly DispatchLineRDto[];
}

export interface DispatchWDto {
  readonly date: string;
  readonly lines: readonly {
    readonly articleId: string;
    readonly quantities: DispatchQuantities;
  }[];
}

/** Bon de transfert généré par destination à la validation (TRF-02). */
export interface DispatchVoucherRDto {
  readonly number: string;
  readonly destination: DispatchDestination;
  readonly lineCount: number;
  readonly units: number;
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
