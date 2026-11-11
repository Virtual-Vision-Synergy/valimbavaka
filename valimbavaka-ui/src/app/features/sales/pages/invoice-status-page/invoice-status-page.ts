import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { InvoiceStatusService } from '../../data-access/invoice-status-service';
import { localIsoDate } from '../../data-access/sales-mapping';

/** Premier jour du mois de `date`, au format `yyyy-MM-dd` (jour local). */
function firstOfMonth(date: Date): string {
  return localIsoDate(new Date(date.getFullYear(), date.getMonth(), 1));
}

/**
 * Suivi des statuts de facture (FAC-05, administrateur) : nombre et montant des factures payées, annulées et
 * contre-passées par un avoir sur une période (mois en cours par défaut), avec le total consolidé.
 */
@Component({
  selector: 'vvs-invoice-status-page',
  imports: [RouterLink, TranslatePipe, AriaryPipe, PageHeader, ListState],
  templateUrl: './invoice-status-page.html',
  styleUrl: './invoice-status-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InvoiceStatusPage {
  private readonly service = inject(InvoiceStatusService);

  protected readonly from = signal(firstOfMonth(new Date()));
  protected readonly to = signal(localIsoDate(new Date()));
  /** Une période à l'envers n'est pas envoyée : le serveur la refuserait (422). */
  protected readonly invalidPeriod = computed(
    () => !!this.from() && !!this.to() && this.from() > this.to(),
  );

  protected readonly totals = rxResource({
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
