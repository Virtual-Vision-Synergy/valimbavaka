import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { IconNode } from 'lucide';

/**
 * Icône Lucide décorative : `<svg [vvsIcon]="icons.Search" [size]="18"></svg>`.
 * Chaque icône est importée nominativement depuis `lucide` (données SVG pures, éliminées si inutilisées).
 * Toujours `aria-hidden` : le sens est porté par le texte ou l'`aria-label` du contrôle parent.
 */
@Component({
  // L'hôte est le <svg> lui-même (aucun élément enveloppe) : sélecteur d'attribut préfixé vvs.
  // eslint-disable-next-line @angular-eslint/component-selector
  selector: 'svg[vvsIcon]',
  templateUrl: './icon.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    xmlns: 'http://www.w3.org/2000/svg',
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    'stroke-linecap': 'round',
    'stroke-linejoin': 'round',
    'aria-hidden': 'true',
    focusable: 'false',
    '[attr.width]': 'size()',
    '[attr.height]': 'size()',
    '[attr.stroke-width]': 'strokeWidth()',
  },
})
export class Icon {
  readonly vvsIcon = input.required<IconNode>();
  readonly size = input(20);
  readonly strokeWidth = input(2);
}
