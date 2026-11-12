import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { REPORT_FIXTURE } from './report-fixtures';
import { ReportDemoFilter, ReportDemoRDto } from './report-models';

const PERIOD_SCALE = { DAY: 0.045, MONTH: 1, YEAR: 9.4 } as const;

/**
 * Section de démonstration de la page des rapports (classement RPT-05) : les montants sont mis à l'échelle selon
 * la période. Le ticket RPT-05 la remplace par l'API.
 */
@Injectable({ providedIn: 'root' })
export class ReportDemoService {
  report(filter: ReportDemoFilter): Observable<ReportDemoRDto> {
    const k = PERIOD_SCALE[filter.granularity];
    const base = REPORT_FIXTURE;
    return demo({
      top: base.top.map((item) => ({
        ...item,
        units: Math.round(item.units * k),
        revenue: Math.round(item.revenue * k),
      })),
    });
  }
}
