import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { StockAlertRDto } from '../../data-access/stock-models';

/**
 * Brouillon du bon de réapprovisionnement (STK-05), regroupé par fournisseur ou par atelier. Les lignes sont les
 * alertes de seuil du serveur (STK-04) : la quantité affichée est celle que le bon enregistrera.
 */
@Component({
  selector: 'vvs-restock-draft',
  imports: [TranslatePipe],
  templateUrl: './restock-draft.html',
  styleUrl: './restock-draft.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RestockDraft {
  readonly alerts = input.required<readonly StockAlertRDto[]>();
  /** Génération en cours : le bouton de validation est désactivé pour éviter un double bon. */
  readonly busy = input(false);
  readonly exported = output();
  readonly validated = output();

  protected readonly groups = computed(() => {
    const groups = new Map<string | null, StockAlertRDto[]>();
    for (const alert of this.alerts()) {
      groups.set(alert.supplierName, [...(groups.get(alert.supplierName) ?? []), alert]);
    }
    return [...groups.entries()].map(([supplier, alerts]) => ({
      supplier,
      leadTimeDays: alerts[0].leadTimeDays,
      expectedDelivery: formatDate(alerts[0].expectedDeliveryDate),
      lines: alerts.map((alert) => ({
        id: alert.articleId,
        name: alert.name,
        quantity: alert.suggestedQuantity,
      })),
    }));
  });
}

/** `yyyy-MM-dd` (jour de Madagascar) → « 15 nov. » ; la date est lue telle quelle, sans fuseau. */
function formatDate(isoDate: string | null): string | null {
  if (isoDate === null) {
    return null;
  }
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' }).format(
    new Date(year, month - 1, day),
  );
}
