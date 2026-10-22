import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { articleInitials } from '../../../../shared/components/article-thumb/article-thumb';
import { RangeTag } from '../../../../shared/components/range-tag/range-tag';
import { ProductRange } from '../../../../shared/models/tone';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';

/** Aperçu de la carte en caisse, mis à jour pendant la saisie (ART-02). */
@Component({
  selector: 'vvs-article-preview',
  imports: [TranslatePipe, AriaryPipe, RangeTag],
  templateUrl: './article-preview.html',
  styleUrl: './article-preview.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ArticlePreview {
  readonly name = input.required<string>();
  readonly price = input.required<number | null>();
  readonly range = input.required<ProductRange | null>();

  protected readonly initials = computed(() => articleInitials(this.name()) || '?');
}
