import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { SubmitResult } from '../../../core/offline/outbox-models';
import { OutboxService } from '../../../core/offline/outbox-service';
import { closingExtras } from './day-fixtures';
import {
  BusinessDayRDto,
  DayClosing,
  DayClosingCountsWDto,
  DayClosingRDto,
  DayOpeningGrid,
  DayOpeningRDto,
  DayOpeningWDto,
  toDayClosing,
  toOpeningGrid,
} from './day-models';

/**
 * Journée de vente. L'ouverture (JR-01) est branchée sur l'API : grille `GET /api/v1/days/opening`, ouverture
 * `POST /api/v1/days` par la file hors ligne (`OutboxService`, SYS-07). La réconciliation de clôture (JR-04) est
 * branchée sur `GET /api/v1/days/closing`, le comptage du soir avec ses motifs d'écart (JR-05) sur
 * `PUT /api/v1/days/closing/counts` par la file hors ligne, le verrouillage (JR-06) sur
 * `POST /api/v1/days/closing/lock` ; la caisse et la liste de contrôle restent en données de démonstration.
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

  /**
   * Enregistre le comptage du soir : le serveur refuse (422) un écart sans motif ; `queued` si la saisie attend le
   * réseau, sinon `body` porte la réconciliation relue avec les comptages enregistrés.
   */
  saveCounts(request: DayClosingCountsWDto): Observable<SubmitResult<DayClosing>> {
    return this.outbox
      .submit<DayClosingRDto>('PUT', `${this.baseUrl}/closing/counts`, request)
      .pipe(map(toClosingResult));
  }

  /**
   * Verrouille la clôture du jour (JR-06), sans corps : le serveur relit les écarts et refuse (422) une ligne non
   * comptée ou un écart sans motif, (409) une journée non ouverte ou déjà clôturée. `queued` si la saisie attend le
   * réseau, sinon `body` porte la réconciliation de la journée clôturée (`status` `CLOSED`).
   */
  lock(): Observable<SubmitResult<DayClosing>> {
    return this.outbox
      .submit<DayClosingRDto>('POST', `${this.baseUrl}/closing/lock`)
      .pipe(map(toClosingResult));
  }
}

function toClosingResult(result: SubmitResult<DayClosingRDto>): SubmitResult<DayClosing> {
  return {
    queued: result.queued,
    body: result.body ? toDayClosing(result.body, closingExtras()) : undefined,
  };
}
