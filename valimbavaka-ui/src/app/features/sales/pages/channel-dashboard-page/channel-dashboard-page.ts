import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ChannelDashboardService } from '../../data-access/channel-dashboard-service';
import { localIsoDate } from '../../data-access/sales-mapping';

/** Premier jour du mois de `date`, au format `yyyy-MM-dd` (jour local). */
function firstOfMonth(date: Date): string {
  return localIsoDate(new Date(date.getFullYear(), date.getMonth(), 1));
}

/**
 * Tableau de bord des ventes par canal (VTE-08, administrateur) : chiffre d'affaires, nombre de ventes validées et part
 * du chiffre d'affaires de chaque canal sur une période (mois en cours par défaut).
 */
@Component({
  selector: 'vvs-channel-dashboard-page',
  imports: [RouterLink, TranslatePipe, AriaryPipe, PageHeader, ListState],
  templateUrl: './channel-dashboard-page.html',
  styleUrl: './channel-dashboard-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChannelDashboardPage {
  private readonly service = inject(ChannelDashboardService);

  protected readonly from = signal(firstOfMonth(new Date()));
  protected readonly to = signal(localIsoDate(new Date()));
  /** Une période à l'envers n'est pas envoyée : le serveur la refuserait (422). */
  protected readonly invalidPeriod = computed(
    () => !!this.from() && !!this.to() && this.from() > this.to(),
  );

  protected readonly dashboard = rxResource({
    params: () => (this.invalidPeriod() ? undefined : { from: this.from(), to: this.to() }),
    stream: ({ params }) => this.service.get(params),
  });

  protected onFromChange(event: Event): void {
    this.from.set((event.target as HTMLInputElement).value);
  }

  protected onToChange(event: Event): void {
    this.to.set((event.target as HTMLInputElement).value);
  }
}
