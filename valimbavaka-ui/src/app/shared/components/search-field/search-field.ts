import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  model,
  output,
  viewChild,
} from '@angular/core';
import { IconNode, Search } from 'lucide';
import { Icon } from '../icon/icon';

/** Champ de recherche avec icône, libellé accessible et raccourci clavier affiché (« F2 »). */
@Component({
  selector: 'vvs-search-field',
  imports: [Icon],
  templateUrl: './search-field.html',
  styleUrl: './search-field.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchField {
  readonly label = input.required<string>();
  readonly placeholder = input('');
  readonly shortcut = input<string>();
  readonly icon = input<IconNode>(Search);
  readonly value = model('');
  /** Entrée validée (douchette code-barres : le scanner envoie « Entrée » après le code). */
  readonly submitted = output<string>();

  private readonly field = viewChild.required<ElementRef<HTMLInputElement>>('field');

  /** Donne le focus au champ et sélectionne son contenu (raccourci clavier de la page). */
  focus(): void {
    this.field().nativeElement.focus();
    this.field().nativeElement.select();
  }
}
