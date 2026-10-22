import { SaleChannel } from '../../../shared/models/commerce';

export type CommissionStatus = 'CALCULATED' | 'VALIDATED' | 'PAID';

/** Les prestations du salon sont rémunérées au forfait, pas au pourcentage. */
export type CommissionChannel = SaleChannel | 'SALON';

/** Commission d'une vente (COM-03). */
export interface CommissionLineRDto {
  readonly sale: string;
  readonly date: string;
  readonly channel: SaleChannel;
  readonly base: number;
  readonly ratePercent: number;
  readonly amount: number;
  readonly status: CommissionStatus;
}

/** Sous-total par canal (COM-02) : `ratePercent` nul pour le forfait salon. */
export interface CommissionChannelRDto {
  readonly channel: CommissionChannel;
  readonly ratePercent: number | null;
  readonly amount: number;
  /** Ventes de la période pour ce canal, ou nombre de prestations pour le salon. */
  readonly base: number;
}

/** Relevé mensuel d'une vendeuse (COM-01 à COM-04). */
export interface CommissionStatementRDto {
  readonly month: string;
  readonly seller: string;
  readonly sales: number;
  readonly salesCount: number;
  readonly goal: number;
  readonly validated: number;
  readonly toValidate: number;
  readonly workingDaysLeft: number;
  readonly channels: readonly CommissionChannelRDto[];
  readonly lines: readonly CommissionLineRDto[];
}
