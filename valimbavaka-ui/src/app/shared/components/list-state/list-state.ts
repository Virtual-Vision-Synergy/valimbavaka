import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { CircleAlert, Inbox, RefreshCw } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../icon/icon';

export type ListStateKind = 'loading' | 'empty' | 'error';

/** États d'une liste (maquette « États de liste ») : chargement en squelette, vide, erreur avec réessai. */
@Component({
  selector: 'vvs-list-state',
  imports: [TranslatePipe, Icon],
  templateUrl: './list-state.html',
  styleUrl: './list-state.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ListState {
  protected readonly icons = { CircleAlert, Inbox, RefreshCw };
  readonly kind = input.required<ListStateKind>();
  /** Clés de traduction ; à défaut, les textes génériques `listState.*`. */
  readonly titleKey = input<string>();
  readonly messageKey = input<string>();
  readonly rows = input(5);
  readonly retry = output();

  protected readonly skeletonRows = computed(() =>
    Array.from({ length: this.rows() }, (_, i) => i),
  );
}
