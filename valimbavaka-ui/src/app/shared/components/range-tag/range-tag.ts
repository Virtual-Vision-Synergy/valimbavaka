import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { ProductRange } from '../../models/tone';

/** Étiquette de gamme (Botanika, Zezika, Solika) aux couleurs de la charte. */
@Component({
  selector: 'vvs-range-tag',
  template: `<span class="tag" [attr.data-range]="range()">{{ range() }}</span>`,
  styleUrl: './range-tag.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RangeTag {
  readonly range = input.required<ProductRange>();
}
