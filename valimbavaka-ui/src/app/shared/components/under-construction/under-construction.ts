import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Construction } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../icon/icon';

/** Marqueur des écrans alimentés par des données de démonstration : le backend du module n'existe pas encore. */
@Component({
  selector: 'vvs-under-construction',
  imports: [TranslatePipe, Icon],
  template: `
    <span class="marker" [attr.title]="'common.underConstructionHint' | translate">
      <svg [vvsIcon]="icons.Construction" [size]="14"></svg>
      {{ 'common.underConstruction' | translate }}
      <span class="sr-only">— {{ 'common.underConstructionHint' | translate }}</span>
    </span>
  `,
  styleUrl: './under-construction.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UnderConstruction {
  protected readonly icons = { Construction };
}
