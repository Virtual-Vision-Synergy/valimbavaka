import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { closingFixture, openingFixture } from './day-fixtures';
import {
  DISPATCH_DESTINATIONS,
  DayClosingRDto,
  DayLockWDto,
  DayOpeningRDto,
  DispatchVoucherRDto,
  DispatchWDto,
} from './day-models';

/** Premier numéro de bon de transfert de démonstration. */
const FIRST_VOUCHER = 93;

/**
 * Journée de vente. Provisoire : données de démonstration ; à terme `GET /api/v1/days/{date}/opening`,
 * `POST /api/v1/days/{date}/dispatch`, `GET /api/v1/days/{date}/closing` et `POST .../lock`.
 */
@Injectable({ providedIn: 'root' })
export class DayService {
  opening(): Observable<DayOpeningRDto> {
    return demo(openingFixture());
  }

  /** Simulation : un bon par destination non vide, comme le fera le serveur. */
  dispatch(request: DispatchWDto): Observable<readonly DispatchVoucherRDto[]> {
    return demo(request).pipe(
      map(({ lines }) =>
        DISPATCH_DESTINATIONS.map((destination) => ({
          destination,
          lineCount: lines.filter((line) => line.quantities[destination] > 0).length,
          units: lines.reduce((sum, line) => sum + line.quantities[destination], 0),
        }))
          .filter((voucher) => voucher.units > 0)
          .map((voucher, index) => ({ ...voucher, number: `TRF-0${FIRST_VOUCHER + index}` })),
      ),
    );
  }

  closing(): Observable<DayClosingRDto> {
    return demo(closingFixture());
  }

  /** Simulation : renvoie l'heure de verrouillage. */
  lock(request: DayLockWDto): Observable<string> {
    return demo(request).pipe(map(() => new Date().toTimeString().slice(0, 5)));
  }
}
