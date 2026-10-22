import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { PRODUCTION_FIXTURE } from './production-fixtures';
import { LotWDto, ProductionContextRDto } from './production-models';

/**
 * Production. Provisoire : données de démonstration ; à terme `GET /api/v1/production/context`
 * et `POST /api/v1/production/lots` (le serveur contrôle les stocks et calcule le coût).
 */
@Injectable({ providedIn: 'root' })
export class ProductionService {
  context(): Observable<ProductionContextRDto> {
    return demo(PRODUCTION_FIXTURE);
  }

  /** Simulation : renvoie le numéro attribué au lot. */
  createLot(lot: LotWDto): Observable<string> {
    return demo(lot).pipe(map(() => PRODUCTION_FIXTURE.nextNumber));
  }
}
