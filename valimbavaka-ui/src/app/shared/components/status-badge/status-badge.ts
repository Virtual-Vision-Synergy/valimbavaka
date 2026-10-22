import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Tone } from '../../models/tone';

/** Pastille d'état : point + libellé, la couleur ne porte jamais seule l'information (WCAG 1.4.1). */
@Component({
  selector: 'vvs-status-badge',
  template: `<span [class]="'badge badge-dot badge-' + tone()">{{ label() }}</span>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusBadge {
  readonly tone = input.required<Tone>();
  readonly label = input.required<string>();
}
