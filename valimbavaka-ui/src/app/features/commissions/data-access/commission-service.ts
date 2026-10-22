import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { statementFixture } from './commission-fixtures';
import { CommissionStatementRDto } from './commission-models';

/** Commissions de la vendeuse connectée. Provisoire ; à terme `GET /api/v1/commissions/mine?month=`. */
@Injectable({ providedIn: 'root' })
export class CommissionService {
  statement(month: string): Observable<CommissionStatementRDto> {
    return demo(statementFixture(month));
  }
}
