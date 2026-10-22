import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { UnderConstruction } from '../under-construction/under-construction';

/**
 * En-tête de page : titre (h1 unique), codes écran, sous-titre et actions projetées.
 * Le marqueur « En construction » apparaît quand la route porte `data: { underConstruction: true }`.
 */
@Component({
  selector: 'vvs-page-header',
  imports: [UnderConstruction],
  templateUrl: './page-header.html',
  styleUrl: './page-header.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PageHeader {
  readonly heading = input.required<string>();
  readonly subtitle = input<string>();
  readonly codes = input<readonly string[]>([]);

  protected readonly underConstruction = inject(ActivatedRoute).snapshot.pathFromRoot.some(
    (route) => route.data['underConstruction'] === true,
  );
}
