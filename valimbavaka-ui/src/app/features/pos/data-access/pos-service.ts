import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { POS_FIXTURE } from './pos-fixtures';
import { PosContextRDto } from './pos-models';

/**
 * Contexte du poste de caisse. Provisoire : données de démonstration ; à terme
 * `GET /api/v1/pos/context` puis `POST /api/v1/sales` (file hors ligne), sans changer l'appelant.
 */
@Injectable({ providedIn: 'root' })
export class PosService {
  context(): Observable<PosContextRDto> {
    return demo(POS_FIXTURE);
  }
}
