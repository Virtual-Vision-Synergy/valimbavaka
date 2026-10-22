/** Nature d'un fournisseur (miroir de l'énumération backend SupplierType). */
export type SupplierType = 'RAW_MATERIAL' | 'PACKAGING' | 'FINISHED' | 'IMPORT';

export const SUPPLIER_TYPES: readonly SupplierType[] = [
  'RAW_MATERIAL',
  'PACKAGING',
  'FINISHED',
  'IMPORT',
];

/** Ligne de la liste des fournisseurs (FRS-01). */
export interface SupplierRDto {
  readonly id: string;
  readonly code: string;
  readonly name: string;
  readonly type: SupplierType;
  readonly city: string;
  readonly taxId: string;
  readonly leadTimeDays: number;
  readonly lastReceptionOn: string;
  readonly active: boolean;
}

export interface SupplierArticleRDto {
  readonly reference: string;
  readonly name: string;
  readonly lastUnitCost: number;
}

export interface SupplierReceptionRDto {
  readonly number: string;
  readonly label: string;
  readonly receivedOn: string;
}

/** Fiche fournisseur (FRS-02 à FRS-04). */
export interface SupplierDetailRDto extends SupplierRDto {
  readonly contactName: string;
  readonly phone: string;
  readonly email: string;
  readonly address: string;
  readonly transport: 'ROAD' | 'SEA' | 'LOCAL';
  readonly articles: readonly SupplierArticleRDto[];
  readonly receptions: readonly SupplierReceptionRDto[];
}
