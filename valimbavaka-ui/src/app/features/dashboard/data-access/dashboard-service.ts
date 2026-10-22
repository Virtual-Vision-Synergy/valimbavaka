import { Injectable } from '@angular/core';
import { Observable, delay, of } from 'rxjs';
import { DASHBOARD_FIXTURE } from './dashboard-fixtures';
import { DashboardRDto } from './dashboard-models';

/** Latence simulée : rend visibles les états de chargement tant que l'API n'existe pas. */
const DEMO_LATENCY_MS = 350;

/**
 * Accès au cockpit du jour. Provisoire : renvoie les données de démonstration ; à terme
 * `this.http.get<DashboardRDto>('/api/v1/dashboard')`, sans changer l'appelant.
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  get(): Observable<DashboardRDto> {
    return of(DASHBOARD_FIXTURE).pipe(delay(DEMO_LATENCY_MS));
  }
}
