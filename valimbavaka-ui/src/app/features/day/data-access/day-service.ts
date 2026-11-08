import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { demo } from '../../../shared/data-access/demo';
import { closingFixture } from './day-fixtures';
import {
  BusinessDayRDto,
  DayClosingRDto,
  DayLockWDto,
  DayOpeningGrid,
  DayOpeningRDto,
  DayOpeningWDto,
  toOpeningGrid,
} from './day-models';

/**
 * Journée de vente. L'ouverture (JR-01) est branchée sur l'API : grille `GET /api/v1/days/opening`, ouverture
 * `POST /api/v1/days` par la file hors ligne (`OutboxService`, SYS-07). La clôture reste en données de démonstration
 * jusqu'à JR-04 / JR-06.
 */
@Injectable({ providedIn: 'root' })
export class DayService {
  private readonly http = inject(HttpClient);
  private readonly outbox = inject(OutboxService);
  private readonly baseUrl = '/api/v1/days';

  /** Grille du jour : articles disponibles à l'entrepôt, colonnes des destinations, bons si déjà ouverte. */
  opening(): Observable<DayOpeningGrid> {
    return this.http.get<DayOpeningRDto>(`${this.baseUrl}/opening`).pipe(map(toOpeningGrid));
  }

  /** Ouvre la journée : un transfert du matin par destination ; `queued` si la saisie attend le réseau. */
  open(body: DayOpeningWDto): Observable<SubmitResult<BusinessDayRDto>> {
    return this.outbox.submit<BusinessDayRDto>('POST', this.baseUrl, body);
  }

  closing(): Observable<DayClosingRDto> {
    return demo(closingFixture());
  }

  /** Simulation : renvoie l'heure de verrouillage. */
  lock(request: DayLockWDto): Observable<string> {
    return demo(request).pipe(map(() => new Date().toTimeString().slice(0, 5)));
  }
}
