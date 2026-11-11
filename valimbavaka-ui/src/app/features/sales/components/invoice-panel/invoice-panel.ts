import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { Printer } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../../../../shared/components/icon/icon';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { toInvoicePreview } from '../../data-access/sales-mapping';
import { SaleSummaryRDto } from '../../data-access/sales-models';
import { InvoiceDocument } from '../invoice-document/invoice-document';
import { INVOICE_TONES } from '../invoice-tones';

/**
 * Panneau facture de la vente sélectionnée (accès facture, VTE-07) : aperçu papier construit depuis la vente tant
 * que les factures ne sont pas émises par le serveur (FAC-01 à FAC-05), et actions.
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

  protected readonly icons = { Printer };
  protected readonly tones = INVOICE_TONES;
  readonly sale = input.required<SaleSummaryRDto>();

  protected readonly invoice = computed(() => toInvoicePreview(this.sale()));

  protected print(): void {
    window.print();
  }

  protected demoAction(): void {
    this.toast.show('common.demoAction', {}, 'info');
  }
}
