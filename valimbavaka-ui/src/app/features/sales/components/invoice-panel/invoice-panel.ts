import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { Printer } from 'lucide';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { errorMessage } from '../../../../core/http/problem-detail';
import { formatAriary } from '../../../../shared/pipes/ariary-pipe';
import { Icon } from '../../../../shared/components/icon/icon';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { InvoiceDelivery } from '../../data-access/invoice-delivery';
import { InvoiceService } from '../../data-access/invoice-service';
import { localDateTime, toInvoicePreview, toInvoiceView } from '../../data-access/sales-mapping';
import { InvoiceRDto, SaleSummaryRDto } from '../../data-access/sales-models';
import { CreditNoteRDto } from '../../data-access/credit-note-models';
import { CreditNoteDialog } from '../credit-note-dialog/credit-note-dialog';
import { CreditNoteNotice } from '../credit-note-notice/credit-note-notice';
import { InvoiceDocument } from '../invoice-document/invoice-document';
import { INVOICE_TONES } from '../invoice-tones';

/**
 * Panneau facture de la vente sélectionnée (accès facture, VTE-07) : la facture numérotée émise à l'encaissement
 * (FAC-01) et les actions. Une vente sans facture (antérieure à la facturation automatique, ou d'une collègue) montre
 * l'aperçu construit depuis la vente. « Régénérer » (FAC-02) rend la même facture (même numéro, même contenu) ou, pour
 * une vente sans facture, l'émet : jamais de saisie libre. Les 3 actions de remise (FAC-03) : « Imprimer » (impression
 * du navigateur), « PDF » (fichier du serveur, tel qu'émis) et « Envoyer » (partage du PDF par le système, ou message
 * `mailto:`) ; PDF et Envoyer exigent une facture émise, et chacune confirme son exécution par un message.
 */
@Component({
  selector: 'vvs-invoice-panel',
  imports: [TranslatePipe, Icon, StatusBadge, InvoiceDocument, CreditNoteDialog, CreditNoteNotice],
  templateUrl: './invoice-panel.html',
  styleUrl: './invoice-panel.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InvoicePanel {
  private readonly toast = inject(ToastService);
  private readonly invoiceService = inject(InvoiceService);
  private readonly delivery = inject(InvoiceDelivery);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly icons = { Printer };
  protected readonly tones = INVOICE_TONES;
  readonly sale = input.required<SaleSummaryRDto>();
  readonly credited = output<CreditNoteRDto>();
  protected readonly regenerating = signal(false);
  protected readonly working = signal(false);
  protected readonly creditDialogOpen = signal(false);

  protected readonly issued = rxResource({
    params: () => this.sale().id,
    stream: ({ params }) => this.invoiceService.getBySale(params),
  });
  protected readonly invoice = computed(() =>
    this.issued.hasValue() ? toInvoiceView(this.issued.value()) : toInvoicePreview(this.sale()),
  );

  protected print(): void {
    window.print();
    this.toast.show('sales.invoice.printed', {
      number: this.invoice().number ?? this.invoice().saleNumber,
    });
  }

  /** « PDF » : enregistre le fichier de la facture émise. */
  protected downloadPdf(): void {
    this.fetchPdf((file, invoice) => {
      this.delivery.save(file, file.name);
      this.toast.show('sales.invoice.pdfSaved', { number: invoice.number });
    });
  }

  /** « Envoyer » : partage le PDF par le système ; sans partage de fichier, enregistre le PDF et ouvre un `mailto:`. */
  protected send(): void {
    this.fetchPdf(async (file, invoice) => {
      const params = {
        number: invoice.number,
        date: localDateTime(invoice.issuedAt).slice(0, 10),
        total: formatAriary(invoice.total),
        issuer: invoice.issuerName,
      };
      const subject = this.translate.instant('sales.invoice.mailSubject', params) as string;
      const body = this.translate.instant('sales.invoice.mailBody', params) as string;
      if (this.delivery.canShare(file)) {
        await this.delivery.share(file, subject, body).then(
          () => this.toast.show('sales.invoice.shared', params),
          () => undefined, // la vendeuse a abandonné le menu de partage : rien n'a été envoyé
        );
        return;
      }
      this.delivery.save(file, file.name);
      this.delivery.openMail(subject, body);
      this.toast.show('sales.invoice.mailOpened', params);
    });
  }

  private fetchPdf(use: (file: File, invoice: InvoiceRDto) => void | Promise<void>): void {
    if (!this.issued.hasValue()) {
      return;
    }
    const invoice = this.issued.value();
    this.working.set(true);
    this.invoiceService
      .downloadPdf(invoice.id)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.working.set(false)),
      )
      .subscribe({
        next: (blob) =>
          void use(new File([blob], `${invoice.number}.pdf`, { type: 'application/pdf' }), invoice),
        error: (error: unknown) =>
          this.toast.show('sales.invoice.pdfFailed', { message: errorMessage(error) }, 'danger'),
      });
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

  protected onCredited(creditNote: CreditNoteRDto): void {
    this.creditDialogOpen.set(false);
    this.toast.show('sales.credit.issued', { number: creditNote.number });
    this.issued.reload();
    this.credited.emit(creditNote);
  }

  protected demoAction(): void {
    this.toast.show('common.demoAction', {}, 'info');
  }
}
