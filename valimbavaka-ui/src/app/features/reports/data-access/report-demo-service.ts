import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { demo } from '../../../shared/data-access/demo';
import { REPORT_FIXTURE } from './report-fixtures';
import { ReportFilterWDto, ReportRDto } from './report-models';

const CHANNEL_SHARE = { ALL: 1, SHOP: 0.54, FACEBOOK: 0.34, PHONE: 0.12 } as const;
const PERIOD_SCALE = { DAY: 0.045, MONTH: 1, YEAR: 9.4 } as const;

/** Facteur de démonstration appliqué aux montants selon les filtres. */
function factor(filter: ReportFilterWDto): number {
  const seller = filter.seller === 'ALL' ? 1 : 0.4;
  return CHANNEL_SHARE[filter.channel] * PERIOD_SCALE[filter.period] * seller;
}

/**
 * Rapports. Provisoire : les montants de démonstration sont mis à l'échelle selon les filtres ;
 * à terme `GET /api/v1/reports?period=&channel=&range=&seller=`.
 */
@Injectable({ providedIn: 'root' })
export class ReportService {
  report(filter: ReportFilterWDto): Observable<ReportRDto> {
    const scale = factor(filter);
    const rangeScale = filter.range === 'ALL' ? 1 : 0.36;
    const k = scale * rangeScale;
    const base = REPORT_FIXTURE;
    return demo({
      kpis: {
        revenue: scaled(base.kpis.revenue, k),
        sales: scaled(base.kpis.sales, k),
        basket: base.kpis.basket,
        delivery: base.kpis.delivery,
      },
      points: base.points.map((p) => ({ ...p, current: p.current * k, previous: p.previous * k })),
      ranges: base.ranges
        .filter((item) => filter.range === 'ALL' || item.range === filter.range)
        .map((item) => ({ ...item, amount: item.amount * scale })),
      top: base.top.map((item) => ({
        ...item,
        units: Math.round(item.units * k),
        revenue: Math.round(item.revenue * k),
      })),
    });
  }
}

function scaled(kpi: { value: number; previous: number }, k: number) {
  return { value: Math.round(kpi.value * k), previous: Math.round(kpi.previous * k) };
}
