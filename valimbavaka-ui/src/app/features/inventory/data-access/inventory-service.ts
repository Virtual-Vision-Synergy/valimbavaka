import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { INVENTORY_FIXTURE } from './inventory-fixtures';
import { InventoryCloseWDto, InventoryContextRDto } from './inventory-models';

/**
 * Inventaire. Provisoire : données de démonstration ; à terme `GET /api/v1/inventories/current`,
 * `PUT /api/v1/inventories/{id}/counts` et `POST /api/v1/inventories/{id}/close`.
 */
@Injectable({ providedIn: 'root' })
export class InventoryService {
  current(): Observable<InventoryContextRDto> {
    return demo(INVENTORY_FIXTURE);
  }

  /** Simulation : accepte la clôture et renvoie le numéro de session. */
  close(request: InventoryCloseWDto): Observable<InventoryCloseWDto> {
    return demo(request);
  }
}
