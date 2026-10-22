import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { RangeTag } from '../../../../shared/components/range-tag/range-tag';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { StockAlertRDto } from '../../data-access/dashboard-models';

/** Articles sous le seuil minimum (DSH-03 · STK-04) ; `limit` réduit la liste pour la vue d'ensemble. */
@Component({
  selector: 'vvs-stock-alerts-card',
  imports: [RouterLink, TranslatePipe, RangeTag, StatusBadge],
  templateUrl: './stock-alerts-card.html',
  styleUrl: './stock-alerts-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StockAlertsCard {
  readonly alerts = input.required<readonly StockAlertRDto[]>();
  readonly limit = input<number>();

  protected readonly rows = computed(() => {
    const limit = this.limit();
    const sorted = [...this.alerts()].sort(
      (a, b) => a.quantity / a.threshold - b.quantity / b.threshold,
    );
    return (limit ? sorted.slice(0, limit) : sorted).map((alert) => ({
      ...alert,
      fill: Math.min(100, (alert.quantity / alert.threshold) * 100),
      out: alert.quantity === 0,
    }));
  });
}
