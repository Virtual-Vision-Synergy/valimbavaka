import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ProductRange } from '../../models/tone';

/** Initiales d'un article : premières lettres des deux premiers mots (« Savon noir » → SN). */
export function articleInitials(name: string): string {
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((word) => word.charAt(0).toUpperCase())
    .join('');
}

/** Vignette d'article aux couleurs de sa gamme (en attendant les photos) ; décorative. */
@Component({
  selector: 'vvs-article-thumb',
  template: `<span class="thumb" [attr.data-range]="range()" [attr.data-size]="size()">{{
    initials()
  }}</span>`,
  styleUrl: './article-thumb.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { 'aria-hidden': 'true' },
})
export class ArticleThumb {
  readonly name = input.required<string>();
  readonly range = input<ProductRange | null>(null);
  readonly size = input<'sm' | 'lg'>('sm');

  protected readonly initials = computed(() => articleInitials(this.name()));
}
