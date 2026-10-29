import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  linkedSignal,
  signal,
} from '@angular/core';
import { rxResource, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Download, Plus, Upload } from 'lucide';
import { HasPermissionDirective } from '../../../../core/auth/has-permission-directive';
import { TranslatePipe } from '@ngx-translate/core';
import { debounceTime, distinctUntilChanged, finalize } from 'rxjs';
import { ArticleThumb } from '../../../../shared/components/article-thumb/article-thumb';
import { ChipOption, FilterChips } from '../../../../shared/components/filter-chips/filter-chips';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { RangeTag } from '../../../../shared/components/range-tag/range-tag';
import { SearchField } from '../../../../shared/components/search-field/search-field';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { downloadFile } from '../../../../shared/data-access/download-file';
import { ProductRange } from '../../../../shared/models/tone';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ReferentialService } from '../../../referential/data-access/referential-service';
import { ArticleType } from '../../../article/data-access/article-models';
import { ArticleService } from '../../../article/data-access/article-service';

type RangeFilter = string;
type TypeFilter = 'ALL' | ArticleType;

const PAGE_SIZE = 10;
const SEARCH_DEBOUNCE_MS = 300;
const EXPORT_FILENAME = 'articles.xlsx';
const ALL_RANGES: ChipOption<RangeFilter> = { value: 'ALL', labelKey: 'articles.filter.allRanges' };
const TYPES: readonly TypeFilter[] = ['ALL', 'FINISHED', 'SEMI_FINISHED', 'RAW_MATERIAL'];
const RANGE_CODES: readonly string[] = ['BOTANIKA', 'ZEZIKA', 'SOLIKA'];

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
    ArticleThumb,
  ],
  templateUrl: './article-list-page.html',
  styleUrl: './article-list-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ArticleListPage {
  private readonly articleService = inject(ArticleService);
  private readonly referentialService = inject(ReferentialService);
  private readonly toast = inject(ToastService);

  protected readonly icons = { Download, Plus, Upload };
  protected readonly types = TYPES;
  protected readonly pageSize = PAGE_SIZE;

  /** Gammes du référentiel (SYS-05) : un échec laisse la liste utilisable, sans le filtre de gamme. */
  private readonly rangeList = rxResource({
    stream: () => this.referentialService.listProductRanges(),
  });
  private readonly rangeItems = computed(() =>
    this.rangeList.hasValue() ? this.rangeList.value() : [],
  );
  protected readonly ranges = computed<readonly ChipOption<RangeFilter>[]>(() => [
    ALL_RANGES,
    ...this.rangeItems().map((range) => ({ value: range.id, label: range.label })),
  ]);
  private readonly rangeCodes = computed(
    () => new Map(this.rangeItems().map((range) => [range.id, range.code])),
  );

  protected readonly exporting = signal(false);
  protected readonly query = signal('');
  protected readonly range = signal<RangeFilter>('ALL');
  protected readonly type = signal<TypeFilter>('ALL');
  /** Recherche envoyée au serveur : rognée et temporisée pour ne pas interroger à chaque frappe. */
  private readonly search = toSignal(
    toObservable(this.query).pipe(
      debounceTime(SEARCH_DEBOUNCE_MS),
      distinctUntilChanged((a, b) => a.trim() === b.trim()),
    ),
    { initialValue: '' },
  );
  protected readonly page = linkedSignal({
    source: () => ({ range: this.range(), type: this.type(), search: this.search().trim() }),
    computation: () => 1,
  });

  /** Une nouvelle page ou un nouveau critère annule la requête précédente. */
  protected readonly articles = rxResource({
    params: () => ({
      page: this.page(),
      range: this.range(),
      type: this.type(),
      search: this.search().trim(),
    }),
    stream: ({ params }) =>
      this.articleService.list({
        page: params.page,
        size: PAGE_SIZE,
        sortBy: 'name',
        sortDirection: 'ASC',
        search: params.search,
        type: params.type === 'ALL' ? undefined : params.type,
        rangeId: params.range === 'ALL' ? undefined : params.range,
      }),
  });
  /** `value()` lève une exception tant que la ressource est en erreur : toujours passer par `hasValue()`. */
  protected readonly rows = computed(() =>
    this.articles.hasValue() ? this.articles.value().items : [],
  );
  protected readonly total = computed(() =>
    this.articles.hasValue() ? this.articles.value().totalCount : 0,
  );

  /** Code de la gamme si c'est l'une des trois gammes à couleur, sinon null (étiquette neutre). */
  protected rangeOf(rangeId: string | null): ProductRange | null {
    const code = rangeId ? this.rangeCodes().get(rangeId) : undefined;
    return code && RANGE_CODES.includes(code) ? (code as ProductRange) : null;
  }

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

  /** Télécharge le xlsx avec les critères affichés (ART-08) ; le serveur réserve l'export à l'administrateur. */
  protected exportList(): void {
    const type = this.type();
    const range = this.range();
    this.exporting.set(true);
    this.articleService
      .exportList({
        search: this.search().trim(),
        type: type === 'ALL' ? undefined : type,
        rangeId: range === 'ALL' ? undefined : range,
      })
      .pipe(finalize(() => this.exporting.set(false)))
      .subscribe({
        next: (response) => downloadFile(response, EXPORT_FILENAME),
        error: () => this.toast.show('articles.exportFailed', {}, 'danger'),
      });
  }

  protected demoAction(): void {
    this.toast.show('common.demoAction', {}, 'info');
  }
}
