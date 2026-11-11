import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { REPORT_FIXTURE } from './report-fixtures';
import { ReportDemoFilter, ReportDemoRDto } from './report-models';

const PERIOD_SCALE = { DAY: 0.045, MONTH: 1, YEAR: 9.4 } as const;

/**
 * Sections de démonstration de la page des rapports (courbe RPT-03, gammes RPT-04, classement
 * RPT-05) : les montants sont mis à l'échelle selon la période. Chaque ticket remplace sa section par l'API.
 */
@Injectable({ providedIn: 'root' })
export class ReportDemoService {
  report(filter: ReportDemoFilter): Observable<ReportDemoRDto> {
    const k = PERIOD_SCALE[filter.granularity];
    const base = REPORT_FIXTURE;
    return demo({
      points: base.points.map((p) => ({ ...p, current: p.current * k, previous: p.previous * k })),
      ranges: base.ranges.map((item) => ({ ...item, amount: item.amount * k })),
      top: base.top.map((item) => ({
        ...item,
        units: Math.round(item.units * k),
        revenue: Math.round(item.revenue * k),
      })),
    });
  }
}
