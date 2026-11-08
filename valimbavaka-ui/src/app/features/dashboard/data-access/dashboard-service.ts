import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, delay, of } from 'rxjs';
import { DASHBOARD_FIXTURE } from './dashboard-fixtures';
import { DashboardRDto, RevenueTrendRDto } from './dashboard-models';

/** Latence simulée : rend visibles les états de chargement tant que l'API n'existe pas. */
const DEMO_LATENCY_MS = 350;

/**
 * Accès au cockpit du jour. `get()` est provisoire : il renvoie les données de démonstration ; à terme
 * `this.http.get<DashboardRDto>('/api/v1/dashboard')` (DSH-01), sans changer l'appelant.
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/dashboard';

  get(): Observable<DashboardRDto> {
    return of(DASHBOARD_FIXTURE).pipe(delay(DEMO_LATENCY_MS));
  }

  /** CA des 7 derniers jours et des 4 dernières semaines (DSH-02), réservé à `reports.read`. */
  revenueTrend(): Observable<RevenueTrendRDto> {
    return this.http.get<RevenueTrendRDto>(`${this.baseUrl}/revenue-trend`);
  }
}
