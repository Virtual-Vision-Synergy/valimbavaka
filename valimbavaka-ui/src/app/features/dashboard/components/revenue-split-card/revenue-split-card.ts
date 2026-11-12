import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { RevenueSplitRDto } from '../../data-access/revenue-split-models';

/** Parts affichées avec une décimale et la virgule française (« 43,8 »). */
const PERCENT = new Intl.NumberFormat('fr-FR', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
});

/** Nombre de couleurs de canal de la charte ; au-delà, elles se répètent. */
const CHANNEL_SLOTS = 3;

/**
 * Répartition du chiffre d'affaires du jour par gamme puis par canal (DSH-04), lue sur l'API : chaque ligne montre
 * sa valeur et sa part relative. Sans chiffre d'affaires (aucune vente validée aujourd'hui), un état vide remplace
 * les barres.
 */
@Component({
  selector: 'vvs-revenue-split-card',
  imports: [TranslatePipe, AriaryPipe, ListState],
  templateUrl: './revenue-split-card.html',
  styleUrl: './revenue-split-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RevenueSplitCard {
  /** `undefined` tant que la lecture n'est pas arrivée. */
  readonly split = input<RevenueSplitRDto | undefined>();
  readonly failed = input(false);
  readonly retry = output();

  protected readonly ranges = computed(() =>
    (this.split()?.byRange ?? []).map((range) => ({
      ...range,
      percent: PERCENT.format(range.sharePercent),
    })),
  );
  protected readonly channels = computed(() =>
    (this.split()?.byChannel ?? []).map((channel, index) => ({
      ...channel,
      slot: index % CHANNEL_SLOTS,
      percent: PERCENT.format(channel.sharePercent),
    })),
  );
  /** Aucun chiffre d'affaires validé aujourd'hui, ni par gamme ni par canal. */
  protected readonly empty = computed(
    () =>
      this.ranges().every((range) => range.revenue === 0) &&
      this.channels().every((channel) => channel.revenue === 0),
  );
}
