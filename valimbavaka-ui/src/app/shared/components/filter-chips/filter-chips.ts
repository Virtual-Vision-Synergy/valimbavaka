import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

export interface ChipOption<T extends string> {
  readonly value: T;
  /** Clé de traduction du libellé. */
  readonly labelKey?: string;
  /** Libellé déjà prêt (donnée métier : nom de tournée…), prioritaire sur `labelKey`. */
  readonly label?: string;
  /** Compteur facultatif affiché après le libellé (« Rupture 3 »). */
  readonly count?: number;
}

/** Filtre exclusif en pastilles (gammes, statuts) : boutons bascule `aria-pressed`, groupe nommé. */
@Component({
  selector: 'vvs-filter-chips',
  imports: [TranslatePipe],
  templateUrl: './filter-chips.html',
  styleUrl: './filter-chips.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FilterChips<T extends string> {
  readonly label = input.required<string>();
  readonly options = input.required<readonly ChipOption<T>[]>();
  readonly value = model.required<T>();
}
