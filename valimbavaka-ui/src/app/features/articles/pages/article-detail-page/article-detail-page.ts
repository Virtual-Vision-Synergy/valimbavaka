import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { ChevronLeft, Pencil, Plus } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { HasPermissionDirective } from '../../../../core/auth/has-permission-directive';
import { ArticleThumb } from '../../../../shared/components/article-thumb/article-thumb';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { RangeTag } from '../../../../shared/components/range-tag/range-tag';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { UnderConstruction } from '../../../../shared/components/under-construction/under-construction';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ArticleService } from '../../data-access/article-service';
import { STOCK_STATUS_TONES } from '../../data-access/article-tones';

/** Ventes moyennes quotidiennes calculées sur 30 jours. */
const SALES_WINDOW_DAYS = 30;

/** Fiche article 360° (ART-01, ART-06) : économie, stock par emplacement, lots, promos, journal. */
@Component({
  selector: 'vvs-article-detail-page',
  imports: [
    HasPermissionDirective,
    RouterLink,
    TranslatePipe,
    AriaryPipe,
    Icon,
    ListState,
    RangeTag,
    StatusBadge,
    ArticleThumb,
    UnderConstruction,
  ],
  templateUrl: './article-detail-page.html',
  styleUrl: './article-detail-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ArticleDetailPage {
  private readonly articleService = inject(ArticleService);

  /** Paramètre de route `:id`. */
  readonly id = input.required<string>();

  protected readonly icons = { ChevronLeft, Pencil, Plus };
  protected readonly tones = STOCK_STATUS_TONES;
  protected readonly article = rxResource({
    params: () => this.id(),
    stream: ({ params }) => this.articleService.get(params),
  });

  protected readonly economics = computed(() => {
    const article = this.article.value();
    if (!article) {
      return null;
    }
    const perDay = article.soldLast30Days / SALES_WINDOW_DAYS;
    return {
      margin: article.price
        ? Math.round(((article.price - article.cost) / article.price) * 100)
        : null,
      perDay: Math.round(perDay),
      coverage: perDay ? Math.floor(article.totalStock / perDay) : null,
      value: article.totalStock * article.cost,
      maxLocation: Math.max(1, ...article.stockByLocation.map((item) => item.quantity)),
    };
  });
}
