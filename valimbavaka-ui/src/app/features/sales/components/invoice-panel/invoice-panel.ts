import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { Printer } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { errorMessage } from '../../../../core/http/problem-detail';
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
 * l'aperçu construit depuis la vente. « Régénérer » (FAC-02) rend la même facture (même numéro, même contenu) ou, pour
 * une vente sans facture, l'émet : jamais de saisie libre.
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
  private readonly destroyRef = inject(DestroyRef);

  protected readonly icons = { Printer };
  protected readonly tones = INVOICE_TONES;
  readonly sale = input.required<SaleSummaryRDto>();
  protected readonly regenerating = signal(false);

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

  protected regenerate(): void {
    this.regenerating.set(true);
    this.invoiceService
      .regenerate(this.sale().id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.regenerating.set(false)),
      )
      .subscribe({
        next: (invoice) => {
          this.issued.set(invoice);
          this.toast.show('sales.invoice.regenerated', { number: invoice.number });
        },
        error: (error: unknown) =>
          this.toast.show(
            'sales.invoice.regenerateFailed',
            { message: errorMessage(error) },
            'danger',
          ),
      });
  }

  protected demoAction(): void {
    this.toast.show('common.demoAction', {}, 'info');
  }
}
