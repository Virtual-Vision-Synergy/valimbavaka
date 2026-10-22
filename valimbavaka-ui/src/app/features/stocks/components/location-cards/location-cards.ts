import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { STOCK_LOCATIONS } from '../../../../shared/data-access/demo-catalog';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { StockRowRDto } from '../../data-access/stock-models';
import { LocationFilter, quantityAt } from '../../data-access/stock-levels';

const FILTERS: readonly LocationFilter[] = ['ALL', ...STOCK_LOCATIONS];

/** Cartes par emplacement (unités, valeur au coût, alertes) ; servent aussi de filtre. */
@Component({
  selector: 'vvs-location-cards',
  imports: [TranslatePipe, AriaryPipe],
  templateUrl: './location-cards.html',
  styleUrl: './location-cards.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LocationCards {
  readonly rows = input.required<readonly StockRowRDto[]>();
  readonly selected = model.required<LocationFilter>();

  protected readonly cards = computed(() =>
    FILTERS.map((location) => {
      const rows = this.rows();
      return {
        location,
        units: rows.reduce((sum, row) => sum + quantityAt(row, location), 0),
        value: rows.reduce((sum, row) => sum + quantityAt(row, location) * row.unitCost, 0),
        // Ruptures locales d'articles vendables : ce qu'il faut transférer en priorité.
        alerts:
          location === 'ALL'
            ? 0
            : rows.filter((row) => row.range !== null && row.quantities[location] === 0).length,
      };
    }),
  );
}
