import { SupplierRDto } from '../../../supplier/data-access/supplier-models';

export const SUPPLIERS_URL = '/api/v1/suppliers';

export const supplier = (name: string, type: SupplierRDto['type']): SupplierRDto => ({
  id: `0195f0a0-0000-7000-8000-${name.length.toString().padStart(12, '0')}`,
  name,
  type,
  contactName: 'Rasoa Rakoto',
  phone: '034 11 222 33',
  email: 'contact@fournisseur.mg',
  address: 'Lot II B 14, Antananarivo',
  notes: 'Livre le mardi',
  active: true,
  createdAt: '2026-10-21T08:30:00Z',
  updatedAt: '2026-10-21T08:30:00Z',
  version: 0,
});

export const huilesRavo = supplier('Huiles Ravo', 'LOCAL');
export const provenceEssences = supplier('Provence Essences', 'FOREIGN');

export const pageOf = (items: SupplierRDto[], page = 1, totalCount = items.length) => ({
  items,
  page,
  pageSize: 8,
  totalCount,
  totalPages: Math.max(1, Math.ceil(totalCount / 8)),
});
