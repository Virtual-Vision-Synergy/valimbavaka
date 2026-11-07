import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { HasPermissionDirective } from '../../../../core/auth/has-permission-directive';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { PageResponse } from '../../../../shared/models/page-response';
import { StockAlertRDto } from '../../../stocks/data-access/stock-models';

/**
 * Articles sous leur seuil minimum sur le cockpit (DSH-03), tels que calculés par le serveur (STK-04) :
 * ordre du serveur (ruptures d'abord), chaque article mène à sa fiche. `limit` réduit la liste pour la vue
 * d'ensemble ; `page` absent = chargement, `failed` = erreur avec réessai.
 */
@Component({
  selector: 'vvs-stock-alerts-card',
  imports: [RouterLink, TranslatePipe, HasPermissionDirective, ListState, StatusBadge],
  templateUrl: './stock-alerts-card.html',
  styleUrl: './stock-alerts-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StockAlertsCard {
  readonly page = input<PageResponse<StockAlertRDto>>();
  readonly failed = input(false);
  readonly limit = input<number>();
  readonly retry = output();

  protected readonly total = computed(() => this.page()?.totalCount ?? 0);
  protected readonly rows = computed(() => {
    const alerts = this.page()?.items ?? [];
    const limit = this.limit();
    return (limit ? alerts.slice(0, limit) : alerts).map((alert) => ({
      ...alert,
      fill: Math.min(100, (alert.quantity / alert.threshold) * 100),
      out: alert.quantity <= 0,
    }));
  });
}
