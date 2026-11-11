import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DayLiveRDto } from './day-live-models';

/**
 * Suivi en direct de la journée (JR-03) : `GET /api/v1/days/live` (permission `reports.read`, administrateur).
 * Lecture seule : l'écran la relit à intervalle régulier, rien n'est écrit ni mis en file hors ligne.
 */
@Injectable({ providedIn: 'root' })
export class DayLiveService {
  private readonly http = inject(HttpClient);

  live(): Observable<DayLiveRDto> {
    return this.http.get<DayLiveRDto>('/api/v1/days/live');
  }
}
