import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { ScanBarcode } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { articleInitials } from '../../../../shared/components/article-thumb/article-thumb';
import { ChipOption, FilterChips } from '../../../../shared/components/filter-chips/filter-chips';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { RangeTag } from '../../../../shared/components/range-tag/range-tag';
import { SearchField } from '../../../../shared/components/search-field/search-field';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { PosArticleRDto, PosCategory } from '../../data-access/pos-models';

const CATEGORIES: readonly ChipOption<PosCategory>[] = [
  { value: 'ALL', labelKey: 'pos.catalog.category.ALL' },
  { value: 'FAVORITES', labelKey: 'pos.catalog.category.FAVORITES' },
  { value: 'BOTANIKA', labelKey: 'pos.catalog.category.BOTANIKA' },
  { value: 'ZEZIKA', labelKey: 'pos.catalog.category.ZEZIKA' },
  { value: 'SOLIKA', labelKey: 'pos.catalog.category.SOLIKA' },
  { value: 'BOXES', labelKey: 'pos.catalog.category.BOXES' },
];

/** Seuil d'affichage « n restants » sur la carte article. */
const LOW_STOCK = 3;

function matchesCategory(article: PosArticleRDto, category: PosCategory): boolean {
  switch (category) {
    case 'ALL':
      return true;
    case 'FAVORITES':
      return article.favorite;
    case 'BOXES':
      return article.box;
    default:
      return article.range === category;
  }
}

/** Catalogue de caisse (VTE-02) : recherche/douchette, filtres de gamme, grille de cartes tactiles. */
@Component({
  selector: 'vvs-pos-catalog',
  imports: [TranslatePipe, AriaryPipe, SearchField, FilterChips, RangeTag, ListState],
  templateUrl: './pos-catalog.html',
  styleUrl: './pos-catalog.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PosCatalog {
  protected readonly icons = { ScanBarcode };
  readonly articles = input.required<readonly PosArticleRDto[]>();
  readonly quantities = input.required<ReadonlyMap<string, number>>();
  readonly picked = output<PosArticleRDto>();

  private readonly searchField = viewChild.required(SearchField);

  protected readonly categories = CATEGORIES;
  protected readonly lowStock = LOW_STOCK;
  protected readonly query = signal('');
  protected readonly category = signal<PosCategory>('ALL');
  protected readonly cards = computed(() => {
    const query = this.query().trim().toLowerCase();
    return this.articles()
      .filter((article) => matchesCategory(article, this.category()))
      .filter(
        (article) =>
          !query ||
          article.name.toLowerCase().includes(query) ||
          article.reference.toLowerCase().includes(query) ||
          article.barcode === query,
      )
      .map((article) => ({ article, initials: articleInitials(article.name) }));
  });

  focusSearch(): void {
    this.searchField().focus();
  }

  /** Entrée dans la recherche : un code-barres exact ou un résultat unique est ajouté directement. */
  protected submit(value: string): void {
    const code = value.trim();
    const exact = this.articles().find((article) => article.barcode === code);
    const cards = this.cards();
    const article = exact ?? (cards.length === 1 ? cards[0].article : undefined);
    if (article && article.stock > 0) {
      this.picked.emit(article);
      this.query.set('');
    }
  }
}
