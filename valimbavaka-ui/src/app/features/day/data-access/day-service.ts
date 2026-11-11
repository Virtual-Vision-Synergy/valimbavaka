import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { demo } from '../../../shared/data-access/demo';
import { closingExtras } from './day-fixtures';
import {
  BusinessDayRDto,
  DayClosing,
  DayClosingRDto,
  DayLockWDto,
  DayOpeningGrid,
  DayOpeningRDto,
  DayOpeningWDto,
  toDayClosing,
  toOpeningGrid,
} from './day-models';

/**
 * Journée de vente. L'ouverture (JR-01) est branchée sur l'API : grille `GET /api/v1/days/opening`, ouverture
 * `POST /api/v1/days` par la file hors ligne (`OutboxService`, SYS-07). La réconciliation de clôture (JR-04) est
 * branchée sur `GET /api/v1/days/closing` ; la caisse, la liste de contrôle et le verrouillage restent en données de
 * démonstration jusqu'à JR-05 / JR-06.
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

  /** Réconciliation d'aujourd'hui : composantes par article et emplacement (409 si la journée n'est pas ouverte). */
  closing(): Observable<DayClosing> {
    return this.http
      .get<DayClosingRDto>(`${this.baseUrl}/closing`)
      .pipe(map((closing) => toDayClosing(closing, closingExtras())));
  }

  /** Simulation : renvoie l'heure de verrouillage. */
  lock(request: DayLockWDto): Observable<string> {
    return demo(request).pipe(map(() => new Date().toTimeString().slice(0, 5)));
  }
}
