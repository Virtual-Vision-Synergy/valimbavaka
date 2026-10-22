import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AvatarInitials } from '../../../../shared/components/avatar-initials/avatar-initials';
import { RangeTag } from '../../../../shared/components/range-tag/range-tag';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { SellerPerformanceRDto, TopArticleRDto } from '../../data-access/dashboard-models';

/** Classements du jour : meilleurs articles (CA) et performance des vendeuses vs objectif (DSH-04). */
@Component({
  selector: 'vvs-top-articles-card',
  imports: [TranslatePipe, AvatarInitials, RangeTag, AriaryPipe],
  templateUrl: './top-articles-card.html',
  styleUrl: './top-articles-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TopArticlesCard {
  readonly articles = input.required<readonly TopArticleRDto[]>();
  readonly sellers = input<readonly SellerPerformanceRDto[]>([]);

  protected readonly ranked = computed(() => {
    const sorted = [...this.articles()].sort((a, b) => b.amount - a.amount);
    const best = sorted[0]?.amount || 1;
    return sorted.map((article) => ({ ...article, share: (article.amount / best) * 100 }));
  });
  protected readonly sellerRows = computed(() =>
    this.sellers().map((seller) => ({
      ...seller,
      progress: Math.min(100, Math.round((seller.amount / seller.target) * 100)),
    })),
  );
}
