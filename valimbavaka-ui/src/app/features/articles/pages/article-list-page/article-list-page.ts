import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  linkedSignal,
  signal,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Download, Plus, Upload } from 'lucide';
import { HasPermissionDirective } from '../../../../core/auth/has-permission-directive';
import { TranslatePipe } from '@ngx-translate/core';
import { ArticleThumb } from '../../../../shared/components/article-thumb/article-thumb';
import { ChipOption, FilterChips } from '../../../../shared/components/filter-chips/filter-chips';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { RangeTag } from '../../../../shared/components/range-tag/range-tag';
import { SearchField } from '../../../../shared/components/search-field/search-field';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { StockGauge } from '../../../../shared/components/stock-gauge/stock-gauge';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { ProductRange } from '../../../../shared/models/tone';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ArticleRDto, ArticleType } from '../../data-access/article-models';
import { ArticleService } from '../../data-access/article-service';
import { STOCK_STATUS_TONES } from '../../data-access/article-tones';

type RangeFilter = 'ALL' | ProductRange;
type TypeFilter = 'ALL' | ArticleType;

const PAGE_SIZE = 10;
const RANGES: readonly ChipOption<RangeFilter>[] = [
  { value: 'ALL', labelKey: 'articles.filter.allRanges' },
  { value: 'BOTANIKA', labelKey: 'articles.range.BOTANIKA' },
  { value: 'ZEZIKA', labelKey: 'articles.range.ZEZIKA' },
  { value: 'SOLIKA', labelKey: 'articles.range.SOLIKA' },
];
const TYPES: readonly TypeFilter[] = ['ALL', 'FINISHED', 'RAW_MATERIAL', 'PACKAGING'];

interface ArticleFilters {
  readonly query: string;
  readonly range: RangeFilter;
  readonly type: TypeFilter;
  readonly lowOnly: boolean;
}

function matches(article: ArticleRDto, filters: ArticleFilters): boolean {
  const query = filters.query.trim().toLowerCase();
  return (
    (filters.range === 'ALL' || article.range === filters.range) &&
    (filters.type === 'ALL' || article.type === filters.type) &&
    (!filters.lowOnly || article.status !== 'AVAILABLE') &&
    (!query ||
      article.name.toLowerCase().includes(query) ||
      article.reference.toLowerCase().includes(query) ||
      article.barcode.includes(query))
  );
}

/** Liste des articles (ART-03, ART-05, ART-07, ART-08) : filtres, sélection groupée, pagination. */
@Component({
  selector: 'vvs-article-list-page',
  imports: [
    HasPermissionDirective,
    RouterLink,
    TranslatePipe,
    AriaryPipe,
    Icon,
    PageHeader,
    ListState,
    Pagination,
    SearchField,
    FilterChips,
    RangeTag,
    StatusBadge,
    StockGauge,
    ArticleThumb,
  ],
  templateUrl: './article-list-page.html',
  styleUrl: './article-list-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ArticleListPage {
  private readonly articleService = inject(ArticleService);
  private readonly toast = inject(ToastService);

  protected readonly icons = { Download, Plus, Upload };
  protected readonly ranges = RANGES;
  protected readonly types = TYPES;
  protected readonly tones = STOCK_STATUS_TONES;
  protected readonly pageSize = PAGE_SIZE;
  protected readonly articles = rxResource({ stream: () => this.articleService.list() });

  protected readonly query = signal('');
  protected readonly range = signal<RangeFilter>('ALL');
  protected readonly type = signal<TypeFilter>('ALL');
  protected readonly lowOnly = signal(false);
  private readonly filters = computed<ArticleFilters>(() => ({
    query: this.query(),
    range: this.range(),
    type: this.type(),
    lowOnly: this.lowOnly(),
  }));
  protected readonly filtered = computed(() =>
    (this.articles.value() ?? []).filter((article) => matches(article, this.filters())),
  );
  protected readonly page = linkedSignal({ source: this.filters, computation: () => 1 });
  protected readonly rows = computed(() =>
    this.filtered().slice((this.page() - 1) * PAGE_SIZE, this.page() * PAGE_SIZE),
  );
  protected readonly counts = computed(() => {
    const all = this.articles.value() ?? [];
    return {
      total: all.length,
      finished: all.filter((article) => article.type === 'FINISHED').length,
      raw: all.filter((article) => article.type === 'RAW_MATERIAL').length,
      packaging: all.filter((article) => article.type === 'PACKAGING').length,
    };
  });

  protected readonly selection = signal<ReadonlySet<string>>(new Set());
  protected readonly allRowsSelected = computed(
    () => this.rows().length > 0 && this.rows().every((row) => this.selection().has(row.id)),
  );
  protected readonly someRowsSelected = computed(
    () => !this.allRowsSelected() && this.rows().some((row) => this.selection().has(row.id)),
  );

  protected toggle(id: string): void {
    this.selection.update((current) => {
      const next = new Set(current);
      if (!next.delete(id)) {
        next.add(id);
      }
      return next;
    });
  }

  protected toggleAll(): void {
    const ids = this.rows().map((row) => row.id);
    const selectAll = !this.allRowsSelected();
    this.selection.update((current) => {
      const next = new Set(current);
      ids.forEach((id) => (selectAll ? next.add(id) : next.delete(id)));
      return next;
    });
  }

  protected clearSelection(): void {
    this.selection.set(new Set());
  }

  protected selectType(value: string): void {
    this.type.set(TYPES.find((type) => type === value) ?? 'ALL');
  }

  protected demoAction(): void {
    this.toast.show('common.demoAction', {}, 'info');
  }
}
