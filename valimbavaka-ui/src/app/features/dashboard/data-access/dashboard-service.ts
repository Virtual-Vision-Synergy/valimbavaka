import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { DASHBOARD_DEMO_FIXTURE } from './dashboard-fixtures';
import { DashboardDemoRDto, DashboardRDto, RevenueTrendRDto } from './dashboard-models';
import { RevenueSplitRDto } from './revenue-split-models';

/**
 * Accès au cockpit du jour. `get()` lit les 4 indicateurs (DSH-01) sur `GET /api/v1/dashboard` (permission
 * `reports.read`, administrateur). `revenueTrend()` lit le CA des 7 derniers jours (DSH-02) et `revenueSplit()` la
 * répartition du CA par gamme et canal (DSH-04), chacun à part de l'agrégat (un appel par bloc, un échec ne casse
 * pas les autres). `demoZones()` fournit les zones qui n'ont pas encore d'API.
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  get(): Observable<DashboardRDto> {
    return this.http.get<DashboardRDto>('/api/v1/dashboard');
  }

  /** Répartition du CA du jour par gamme et canal (DSH-04), à part de l'agrégat (même permission). */
  revenueSplit(): Observable<RevenueSplitRDto> {
    return this.http.get<RevenueSplitRDto>('/api/v1/dashboard/revenue-split');
  }

  revenueTrend(): Observable<RevenueTrendRDto> {
    return this.http.get<RevenueTrendRDto>('/api/v1/dashboard/revenue-trend');
  }

  demoZones(): Observable<DashboardDemoRDto> {
    return demo(DASHBOARD_DEMO_FIXTURE);
  }
}
