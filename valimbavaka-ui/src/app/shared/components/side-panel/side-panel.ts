import { DOCUMENT } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  afterRenderEffect,
  inject,
  input,
  output,
  untracked,
  viewChild,
} from '@angular/core';
import { X } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../icon/icon';

/** Largeur à partir de laquelle le panneau de détail reste ancré à côté de la liste. */
export const DOCKED_PANEL_MEDIA = '(min-width: 1600px)';

/** Vrai si le panneau de détail est ancré (écran très large) plutôt qu'en tiroir. */
export function isPanelDocked(): boolean {
  return inject(DOCUMENT).defaultView?.matchMedia?.(DOCKED_PANEL_MEDIA).matches ?? false;
}

/**
 * Panneau de détail d'une liste : ancré à droite sur grand écran, tiroir au-dessus du contenu
 * sinon (fermeture par le bouton ou Échap, focus déplacé à chaque nouvelle sélection).
 */
@Component({
  selector: 'vvs-side-panel',
  imports: [TranslatePipe, Icon],
  templateUrl: './side-panel.html',
  styleUrl: './side-panel.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'dismiss()' },
})
export class SidePanel {
  protected readonly icons = { X };
  protected readonly docked = isPanelDocked();
  private readonly panel = viewChild.required<ElementRef<HTMLElement>>('panel');

  readonly label = input.required<string>();
  /** Identifiant de l'élément affiché : le focus suit chaque changement en mode tiroir. */
  readonly focusKey = input<string | null>(null);
  readonly closed = output<void>();

  constructor() {
    afterRenderEffect(() => {
      this.focusKey();
      if (!this.docked) {
        untracked(() => this.panel().nativeElement.focus());
      }
    });
  }

  protected dismiss(): void {
    if (!this.docked) {
      this.closed.emit();
    }
  }
}
