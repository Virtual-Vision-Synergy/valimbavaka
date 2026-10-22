import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Check } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../icon/icon';

export type StepState = 'done' | 'current' | 'upcoming';

/** Étape décrite par des clés de traduction : le libellé suit la langue choisie. */
export interface StepItem {
  readonly labelKey: string;
  readonly captionKey: string;
  readonly params?: Readonly<Record<string, string | number>>;
  readonly state: StepState;
}

/** Progression ordonnée (journée : ouverture → ventes → livraisons → clôture) ; l'étape en cours est annoncée. */
@Component({
  selector: 'vvs-stepper',
  imports: [TranslatePipe, Icon],
  templateUrl: './stepper.html',
  styleUrl: './stepper.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Stepper {
  protected readonly icons = { Check };
  readonly label = input.required<string>();
  readonly steps = input.required<readonly StepItem[]>();
}
