import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
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
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ArticleService } from '../../data-access/article-service';
import { BATCH_STATUS_TONES, STOCK_STATUS_TONES } from '../../data-access/article-tones';

/**
 * Fiche article 360° (ART-06) : identité, stock par emplacement (STK-01), lots de production où l'article entre ou
 * sort (PRD-01), promotions actives qui le visent (PRO-01) et journal de ses derniers mouvements de stock.
 */
@Component({
  selector: 'vvs-article-detail-page',
  imports: [
    DatePipe,
    HasPermissionDirective,
    RouterLink,
    TranslatePipe,
    AriaryPipe,
    Icon,
    ListState,
    RangeTag,
    StatusBadge,
    ArticleThumb,
  ],
  templateUrl: './article-detail-page.html',
  styleUrl: './article-detail-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ArticleDetailPage {
  private readonly articleService = inject(ArticleService);

  /** Paramètre de route `:id` (UUID de l'article). */
  readonly id = input.required<string>();

  protected readonly icons = { ChevronLeft, Pencil, Plus };
  protected readonly tones = STOCK_STATUS_TONES;
  protected readonly batchTones = BATCH_STATUS_TONES;
  protected readonly sheet = rxResource({
    params: () => this.id(),
    stream: ({ params }) => this.articleService.sheet(params),
  });
}
