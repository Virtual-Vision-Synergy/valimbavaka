import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { DASHBOARD_DEMO_FIXTURE } from './dashboard-fixtures';
import { DashboardDemoRDto, DashboardRDto, RevenueTrendRDto } from './dashboard-models';

/**
 * Accès au cockpit du jour. `get()` lit les 4 indicateurs (DSH-01) sur `GET /api/v1/dashboard` (permission
 * `reports.read`, administrateur). `revenueTrend()` lit le CA des 7 derniers jours (DSH-02), à part de l'agrégat.
 * `demoZones()` fournit les zones sans API : DSH-04 ajoute ici sa propre lecture (un appel par bloc, un échec ne
 * casse pas les autres) et retire ses champs de la démo.
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  get(): Observable<DashboardRDto> {
    return this.http.get<DashboardRDto>('/api/v1/dashboard');
  }

  revenueTrend(): Observable<RevenueTrendRDto> {
    return this.http.get<RevenueTrendRDto>('/api/v1/dashboard/revenue-trend');
  }

  demoZones(): Observable<DashboardDemoRDto> {
    return demo(DASHBOARD_DEMO_FIXTURE);
  }
}
