import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Printer } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { SalesService } from '../../data-access/sales-service';
import { InvoiceDocument } from '../invoice-document/invoice-document';
import { INVOICE_TONES } from '../invoice-tones';

/** Panneau facture de la vente sélectionnée (FAC-01 à FAC-05) : aperçu papier et actions. */
@Component({
  selector: 'vvs-invoice-panel',
  imports: [TranslatePipe, Icon, ListState, StatusBadge, InvoiceDocument],
  templateUrl: './invoice-panel.html',
  styleUrl: './invoice-panel.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InvoicePanel {
  private readonly salesService = inject(SalesService);
  private readonly toast = inject(ToastService);

  protected readonly icons = { Printer };
  protected readonly tones = INVOICE_TONES;
  readonly saleNumber = input.required<string>();

  protected readonly invoice = rxResource({
    params: () => this.saleNumber(),
    stream: ({ params }) => this.salesService.invoice(params),
  });

  protected print(): void {
    window.print();
  }

  protected demoAction(): void {
    this.toast.show('common.demoAction', {}, 'info');
  }
}
