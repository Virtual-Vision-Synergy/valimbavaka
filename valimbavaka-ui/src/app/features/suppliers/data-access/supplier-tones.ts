import { Tone } from '../../../shared/models/tone';
import { SupplierType } from '../../supplier/data-access/supplier-models';

export const SUPPLIER_TYPE_TONES: Readonly<Record<SupplierType, Tone>> = {
  LOCAL: 'secondary',
  FOREIGN: 'warning',
};
