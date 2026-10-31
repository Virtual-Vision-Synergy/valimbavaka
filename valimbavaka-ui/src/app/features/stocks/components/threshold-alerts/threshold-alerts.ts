import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { StockAlertRDto } from '../../data-access/stock-models';

/**
 * Articles sous leur seuil minimum (STK-04) : stock actuel, seuil, manque et quantité suggérée, tels que
 * calculés par le serveur. `total` peut dépasser le nombre de lignes reçues (une page de 100).
 */
@Component({
  selector: 'vvs-threshold-alerts',
  imports: [TranslatePipe],
  templateUrl: './threshold-alerts.html',
  styleUrl: './threshold-alerts.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ThresholdAlerts {
  readonly alerts = input.required<readonly StockAlertRDto[]>();
  readonly total = input.required<number>();
}
