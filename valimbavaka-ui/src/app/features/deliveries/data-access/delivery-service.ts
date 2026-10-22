import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { DELIVERY_BOARD_FIXTURE, DRIVER_ROUND_FIXTURE } from './delivery-fixtures';
import { DeliveryBoardRDto, DeliveryStatusWDto, DriverRoundRDto } from './delivery-models';

/**
 * Livraisons. Provisoire : données de démonstration ; à terme `GET /api/v1/deliveries?date=`,
 * `GET /api/v1/rounds/mine` et `PATCH /api/v1/deliveries/{id}/status` via la file hors ligne.
 */
@Injectable({ providedIn: 'root' })
export class DeliveryService {
  board(): Observable<DeliveryBoardRDto> {
    return demo(DELIVERY_BOARD_FIXTURE);
  }

  myRound(): Observable<DriverRoundRDto> {
    return demo(DRIVER_ROUND_FIXTURE);
  }

  /** Simulation : accepte le changement de statut ; aucune donnée n'est conservée. */
  updateStatus(deliveryId: string, change: DeliveryStatusWDto): Observable<string> {
    return demo(change).pipe(map(() => deliveryId));
  }
}
