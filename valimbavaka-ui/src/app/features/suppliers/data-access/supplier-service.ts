import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { SUPPLIERS_FIXTURE, supplierDetailFixture } from './supplier-fixtures';
import { SupplierDetailRDto, SupplierRDto } from './supplier-models';

/**
 * Fournisseurs. Provisoire : données de démonstration ; à terme `GET /api/v1/suppliers`
 * (paginé, filtres côté serveur) et `GET /api/v1/suppliers/{id}`.
 */
@Injectable({ providedIn: 'root' })
export class SupplierService {
  list(): Observable<readonly SupplierRDto[]> {
    return demo(SUPPLIERS_FIXTURE);
  }

  get(id: string): Observable<SupplierDetailRDto | null> {
    return demo(supplierDetailFixture(id));
  }
}
