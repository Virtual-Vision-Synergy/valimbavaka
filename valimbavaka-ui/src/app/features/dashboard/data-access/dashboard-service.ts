import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { DASHBOARD_DEMO_FIXTURE } from './dashboard-fixtures';
import { DashboardDemoRDto, DashboardRDto } from './dashboard-models';

/**
 * Accès au cockpit du jour. `get()` lit les 4 indicateurs (DSH-01) sur `GET /api/v1/dashboard` (permission
 * `reports.read`, administrateur). `demoZones()` fournit les zones sans API : DSH-02 et DSH-04 ajoutent ici leur
 * propre lecture (un appel par bloc, un échec ne casse pas les autres) et retirent leurs champs de la démo.
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  get(): Observable<DashboardRDto> {
    return this.http.get<DashboardRDto>('/api/v1/dashboard');
  }

  demoZones(): Observable<DashboardDemoRDto> {
    return demo(DASHBOARD_DEMO_FIXTURE);
  }
}
