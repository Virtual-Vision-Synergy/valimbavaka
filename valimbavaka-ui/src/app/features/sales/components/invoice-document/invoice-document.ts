import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { NgOptimizedImage } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { InvoiceRDto } from '../../data-access/sales-models';

/**
 * Facture au format papier (FAC-01) : toujours sur fond blanc, y compris en thème sombre,
 * pour que l'écran montre exactement ce qui sera imprimé ou envoyé.
 */
@Component({
  selector: 'vvs-invoice-document',
  imports: [NgOptimizedImage, TranslatePipe, AriaryPipe],
  templateUrl: './invoice-document.html',
  styleUrl: './invoice-document.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InvoiceDocument {
  readonly invoice = input.required<InvoiceRDto>();
}
