import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Printer } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../../../../shared/components/icon/icon';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { InvoiceService } from '../../data-access/invoice-service';
import { toInvoicePreview, toInvoiceView } from '../../data-access/sales-mapping';
import { SaleSummaryRDto } from '../../data-access/sales-models';
import { InvoiceDocument } from '../invoice-document/invoice-document';
import { INVOICE_TONES } from '../invoice-tones';

/**
 * Panneau facture de la vente sélectionnée (accès facture, VTE-07) : la facture numérotée émise à l'encaissement
 * (FAC-01) et les actions. Une vente sans facture (antérieure à la facturation automatique, ou d'une collègue) montre
 * l'aperçu construit depuis la vente.
 */
@Component({
  selector: 'vvs-invoice-panel',
  imports: [TranslatePipe, Icon, StatusBadge, InvoiceDocument],
  templateUrl: './invoice-panel.html',
  styleUrl: './invoice-panel.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InvoicePanel {
  private readonly toast = inject(ToastService);
  private readonly invoiceService = inject(InvoiceService);

  protected readonly icons = { Printer };
  protected readonly tones = INVOICE_TONES;
  readonly sale = input.required<SaleSummaryRDto>();

  protected readonly issued = rxResource({
    params: () => this.sale().id,
    stream: ({ params }) => this.invoiceService.getBySale(params),
  });
  protected readonly invoice = computed(() =>
    this.issued.hasValue() ? toInvoiceView(this.issued.value()) : toInvoicePreview(this.sale()),
  );

  protected print(): void {
    window.print();
  }

  protected demoAction(): void {
    this.toast.show('common.demoAction', {}, 'info');
  }
}
