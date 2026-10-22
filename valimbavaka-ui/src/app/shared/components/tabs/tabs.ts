import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  model,
  viewChildren,
} from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

export interface TabItem<T extends string> {
  readonly id: T;
  /** Clé de traduction du libellé. */
  readonly labelKey: string;
  /** Pastille de comptage facultative (ex. sorties en cours). */
  readonly count?: number;
}

/**
 * Onglets conformes au motif ARIA « tabs » : focus itinérant, flèches / Début / Fin, activation au focus.
 * Le parent rend le panneau : `role="tabpanel"`, `id="{{prefix}}-panel-{{id}}"`,
 * `aria-labelledby="{{prefix}}-tab-{{id}}"`.
 */
@Component({
  selector: 'vvs-tabs',
  imports: [TranslatePipe],
  templateUrl: './tabs.html',
  styleUrl: './tabs.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Tabs<T extends string> {
  readonly label = input.required<string>();
  readonly prefix = input.required<string>();
  readonly tabs = input.required<readonly TabItem<T>[]>();
  readonly selected = model.required<T>();

  private readonly buttons = viewChildren<ElementRef<HTMLButtonElement>>('tab');

  protected onKeydown(event: KeyboardEvent, index: number): void {
    const count = this.tabs().length;
    const targets: Record<string, number> = {
      ArrowRight: (index + 1) % count,
      ArrowLeft: (index - 1 + count) % count,
      Home: 0,
      End: count - 1,
    };
    const target = targets[event.key];
    if (target === undefined) {
      return;
    }
    event.preventDefault();
    this.selected.set(this.tabs()[target].id);
    this.buttons()[target]?.nativeElement.focus();
  }
}
