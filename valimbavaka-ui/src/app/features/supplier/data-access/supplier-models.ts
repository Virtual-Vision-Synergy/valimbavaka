/** Miroir de SupplierType côté backend. */
export type SupplierType = 'LOCAL' | 'FOREIGN';

/** Miroir de SupplierRDto : fiche fournisseur en lecture. */
export interface SupplierRDto {
  id: string;
  name: string;
  type: SupplierType;
  contactName: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  notes: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

/** Miroir de SupplierWDto : création et modification (ni id ni statut). */
export interface SupplierWDto {
  name: string;
  type: SupplierType;
  contactName: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  notes: string | null;
}

/** Miroir de SupplierStatusWDto. */
export interface SupplierStatusWDto {
  active: boolean;
}
