import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  afterNextRender,
  input,
  output,
  viewChild,
} from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

/**
 * Confirmation d'une action sensible dans un <dialog> modal natif (focus piégé, Échap = annuler).
 * Le parent l'affiche avec `@if` : il s'ouvre à la création et émet `confirmed` ou `cancelled`.
 */
@Component({
  selector: 'vvs-confirm-dialog',
  imports: [TranslatePipe],
  templateUrl: './confirm-dialog.html',
  styleUrl: './confirm-dialog.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmDialog {
  readonly heading = input.required<string>();
  readonly message = input.required<string>();
  readonly confirmLabel = input.required<string>();
  /** Action destructrice : bouton de confirmation en rouge. */
  readonly danger = input(false);
  readonly confirmed = output();
  readonly cancelled = output();

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

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

  protected cancel(): void {
    this.cancelled.emit();
  }
}
