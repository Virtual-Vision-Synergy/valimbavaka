import { ChangeDetectionStrategy, Component, output } from '@angular/core';
import { Delete } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../../../../shared/components/icon/icon';

/** Touche du pavé : chiffres, double zéro ou retour arrière. */
export type KeypadKey = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' | '00' | 'BACK';

const KEYS: readonly KeypadKey[] = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '00', '0', 'BACK'];

/** Pavé numérique tactile de la caisse tablette (montant reçu) ; masqué sur poste à souris. */
@Component({
  selector: 'vvs-pos-keypad',
  imports: [TranslatePipe, Icon],
  templateUrl: './pos-keypad.html',
  styleUrl: './pos-keypad.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PosKeypad {
  protected readonly icons = { Delete };
  protected readonly keys = KEYS;
  readonly pressed = output<KeypadKey>();
}
