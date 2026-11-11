import { PageQuery } from '../../../shared/models/page-response';
import { SaleChannel } from '../../../shared/models/commerce';
import { CommissionBase } from './commission-rule-models';

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

/** Miroir de CommissionSourceType : opération qui rapporte la commission (COM-02). */
export type CommissionSourceType = 'SALE' | 'SERVICE_RECORD';

/** Miroir de CommissionRateSource : niveau d'où vient le taux (le plus précis gagne, sans cumul). */
export type CommissionRateSource = 'SALON_SERVICE' | 'SALES_CHANNEL' | 'SELLER' | 'NONE';

/**
 * Miroir de CommissionRDto : commission calculée par le serveur à l'enregistrement d'une vente ou d'une prestation
 * (COM-02) ; `amount` = `baseAmount` × `rate` / 100, arrondi à l'ariary.
 */
export interface CommissionRDto {
  id: string;
  sourceType: CommissionSourceType;
  sourceId: string;
  /** `VTE-0042` ou `PRE-0007`. */
  sourceNumber: string;
  sellerId: string;
  sellerName: string;
  salesChannelId: string;
  salonServiceId: string | null;
  /** Journée de l'opération, `AAAA-MM-JJ`. */
  businessDate: string;
  revenue: number;
  /** Montant auquel le taux s'applique : le CA, ou la marge pour une règle sur la marge. */
  baseAmount: number;
  base: CommissionBase;
  /** Taux en pourcentage, 2 décimales. */
  rate: number;
  rateSource: CommissionRateSource;
  ruleId: string | null;
  amount: number;
  /**
   * `VALIDATED` une fois validée par l'administrateur (COM-03) ; `CANCELLED` quand la vente est annulée : la commission
   * reste consultable mais ne compte plus.
   */
  status: 'CALCULATED' | 'VALIDATED' | 'CANCELLED';
  createdAt: string;
}

/** Filtres de `GET /api/v1/commissions` : vendeuse et journées `[from, to]` (incluses), tous facultatifs. */
export interface CommissionListQuery extends PageQuery {
  sellerId?: string;
  from?: string;
  to?: string;
}

/** Miroir de SellerCommissionStatus : à valider tant qu'une commission est encore `CALCULATED` (COM-03). */
export type SellerCommissionStatus = 'TO_VALIDATE' | 'VALIDATED';

/** Miroir de SellerCommissionRDto : commissions d'une vendeuse sur une période, les annulées exclues des totaux. */
export interface SellerCommissionRDto {
  sellerId: string;
  sellerName: string;
  status: SellerCommissionStatus;
  toValidateCount: number;
  toValidateAmount: number;
  validatedCount: number;
  validatedAmount: number;
}

/** Corps de `POST /api/v1/commissions/validation` : vendeuse et journées `[from, to]` (incluses). */
export interface CommissionValidationWDto {
  sellerId: string;
  from: string;
  to: string;
}

/** Miroir de CommissionValidationRDto : ce que la validation vient de valider. */
export interface CommissionValidationRDto {
  sellerId: string;
  sellerName: string;
  from: string;
  to: string;
  validatedCount: number;
  validatedAmount: number;
}
