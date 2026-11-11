import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { CreditNoteService } from '../../data-access/credit-note-service';
import { localDateTime } from '../../data-access/sales-mapping';

/**
 * Avoir d'une facture contre-passée (FAC-04) : numéro, date et motif, lus sur le serveur. Une facture sans avoir (404,
 * par exemple annulée avant FAC-04) n'affiche rien.
 */
@Component({
  selector: 'vvs-credit-note-notice',
  imports: [TranslatePipe],
  templateUrl: './credit-note-notice.html',
  styleUrl: './credit-note-notice.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreditNoteNotice {
  private readonly creditNoteService = inject(CreditNoteService);

  readonly invoiceId = input.required<string>();

  protected readonly creditNote = rxResource({
    params: () => this.invoiceId(),
    stream: ({ params }) => this.creditNoteService.getByInvoice(params),
  });

  protected readonly localDateTime = localDateTime;
}
