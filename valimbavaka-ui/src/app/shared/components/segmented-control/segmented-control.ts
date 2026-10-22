import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

export interface SegmentOption<T extends string> {
  readonly value: T;
  /** Clé de traduction du libellé. */
  readonly labelKey: string;
}

/** Choix exclusif compact (période…) : groupe de boutons radio natifs, navigable aux flèches. */
@Component({
  selector: 'vvs-segmented-control',
  imports: [TranslatePipe],
  templateUrl: './segmented-control.html',
  styleUrl: './segmented-control.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SegmentedControl<T extends string> {
  readonly label = input.required<string>();
  readonly options = input.required<readonly SegmentOption<T>[]>();
  readonly value = model.required<T>();

  /** Nom du groupe radio, dérivé du libellé (unique par écran) : pas de compteur global mutable. */
  protected readonly name = computed(
    () => `segment-${this.label().toLowerCase().replace(/\W+/g, '-')}`,
  );
}
