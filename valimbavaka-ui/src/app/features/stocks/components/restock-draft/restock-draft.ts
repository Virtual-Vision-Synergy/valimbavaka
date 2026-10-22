import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { StockRowRDto } from '../../data-access/stock-models';
import { COVERAGE_DAYS, proposedQuantity } from '../../data-access/stock-levels';

const MS_PER_DAY = 86_400_000;
/** Date du jour de démonstration (même journée que les autres écrans). */
const TODAY = new Date(2026, 9, 21);

/** Brouillon de bon de réapprovisionnement (STK-05), regroupé par fournisseur ou par atelier. */
@Component({
  selector: 'vvs-restock-draft',
  imports: [TranslatePipe],
  templateUrl: './restock-draft.html',
  styleUrl: './restock-draft.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RestockDraft {
  readonly rows = input.required<readonly StockRowRDto[]>();
  readonly exported = output();
  readonly validated = output();

  protected readonly coverageDays = COVERAGE_DAYS;
  protected readonly groups = computed(() => {
    const groups = new Map<string | null, StockRowRDto[]>();
    for (const row of this.rows()) {
      groups.set(row.supplierName, [...(groups.get(row.supplierName) ?? []), row]);
    }
    return [...groups.entries()].map(([supplier, rows]) => ({
      supplier,
      leadTimeDays: rows[0].leadTimeDays,
      orderBefore: this.orderBefore(rows[0].leadTimeDays),
      lines: rows.map((row) => ({
        id: row.articleId,
        name: row.name,
        quantity: proposedQuantity(row),
      })),
    }));
  });

  /** Dernier jour pour commander : avant que le délai fournisseur n'entame la couverture restante. */
  private orderBefore(leadTimeDays: number | null): string | null {
    if (leadTimeDays === null) {
      return null;
    }
    const date = new Date(TODAY.getTime() + Math.max(1, 14 - leadTimeDays) * MS_PER_DAY);
    return new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' }).format(date);
  }
}
