import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { DASHBOARD_DEMO_FIXTURE } from './dashboard-fixtures';
import { DashboardDemoRDto, DashboardRDto } from './dashboard-models';
import { RevenueSplitRDto } from './revenue-split-models';

/**
 * Accès au cockpit du jour. `get()` lit les 4 indicateurs (DSH-01) sur `GET /api/v1/dashboard` (permission
 * `reports.read`, administrateur). `revenueSplit()` lit la répartition du CA par gamme et canal (DSH-04), à part de
 * l'agrégat. `demoZones()` fournit les zones sans API : DSH-02 ajoute ici sa propre lecture (un appel par bloc, un
 * échec ne casse pas les autres) et retire ses champs de la démo.
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

  demoZones(): Observable<DashboardDemoRDto> {
    return demo(DASHBOARD_DEMO_FIXTURE);
  }
}
