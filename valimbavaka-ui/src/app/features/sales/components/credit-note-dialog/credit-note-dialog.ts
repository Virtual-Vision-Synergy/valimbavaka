import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { errorMessage } from '../../../../core/http/problem-detail';
import { CREDIT_NOTE_REASON_MAX, CreditNoteRDto } from '../../data-access/credit-note-models';
import { CreditNoteService } from '../../data-access/credit-note-service';

/** Un motif fait d'espaces seuls ne motive rien : refusé comme un motif vide (le serveur applique la même règle). */
function notBlank(control: AbstractControl<string>): ValidationErrors | null {
  return control.value.trim() ? null : { required: true };
}

/**
 * Dialogue d'émission d'un avoir motivé (FAC-04) dans un <dialog> modal natif (focus piégé, Échap = annuler). Le parent
 * l'affiche avec `@if` : il s'ouvre à la création. Le motif est obligatoire ; en cas de refus du serveur (déjà
 * contre-passée, journée clôturée…), le message reste dans le dialogue et rien n'est émis.
 */
@Component({
  selector: 'vvs-credit-note-dialog',
  imports: [ReactiveFormsModule, TranslatePipe],
  templateUrl: './credit-note-dialog.html',
  styleUrl: './credit-note-dialog.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreditNoteDialog {
  private readonly creditNoteService = inject(CreditNoteService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  readonly invoiceId = input.required<string>();
  readonly invoiceNumber = input.required<string>();
  readonly credited = output<CreditNoteRDto>();
  readonly cancelled = output();

  protected readonly maxLength = CREDIT_NOTE_REASON_MAX;
  protected readonly form = inject(FormBuilder).nonNullable.group({
    reason: ['', [notBlank, Validators.maxLength(CREDIT_NOTE_REASON_MAX)]],
  });
  protected readonly submitting = signal(false);
  protected readonly submitted = signal(false);
  protected readonly failure = signal<string | null>(null);

  constructor() {
    afterNextRender(() => {
      const element = this.dialog().nativeElement;
      if (typeof element.showModal === 'function') {
        element.showModal();
      } else {
        element.setAttribute('open', '');
      }
    });
  }

  protected showReasonError(): boolean {
    const reason = this.form.controls.reason;
    return reason.invalid && (reason.touched || this.submitted());
  }

  protected cancel(): void {
    if (!this.submitting()) {
      this.cancelled.emit();
    }
  }

  protected submit(): void {
    this.submitted.set(true);
    if (this.form.invalid || this.submitting()) {
      return;
    }
    this.failure.set(null);
    this.submitting.set(true);
    this.creditNoteService
      .issue(this.invoiceId(), { reason: this.form.controls.reason.value.trim() })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.submitting.set(false)),
      )
      .subscribe({
        next: (creditNote) => this.credited.emit(creditNote),
        error: (error: unknown) => this.failure.set(errorMessage(error)),
      });
  }
}
