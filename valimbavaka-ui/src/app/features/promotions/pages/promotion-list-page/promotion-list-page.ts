import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { errorMessage } from '../../../../core/http/problem-detail';
import { ChipOption, FilterChips } from '../../../../shared/components/filter-chips/filter-chips';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { PromotionCard } from '../../components/promotion-card/promotion-card';
import { PromotionForm } from '../../components/promotion-form/promotion-form';
import {
  PROMOTION_STATUSES,
  PromotionCardView,
  PromotionDraft,
  PromotionStatus,
} from '../../data-access/promotion-models';
import { toCardView } from '../../data-access/promotion-request';
import { PromotionService } from '../../data-access/promotion-service';
import { statusOf } from '../../data-access/promotion-status';

/** Promotions (PRO-01 à PRO-04) : appliquées automatiquement en caisse quand les conditions sont réunies. */
@Component({
  selector: 'vvs-promotion-list-page',
  imports: [TranslatePipe, PageHeader, ListState, FilterChips, PromotionCard, PromotionForm],
  templateUrl: './promotion-list-page.html',
  styleUrl: './promotion-list-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromotionListPage {
  private readonly promotionService = inject(PromotionService);
  private readonly toast = inject(ToastService);

  protected readonly context = rxResource({ stream: () => this.promotionService.context() });
  protected readonly created = signal<readonly PromotionCardView[]>([]);
  protected readonly status = signal<PromotionStatus | 'ALL'>('ALL');
  protected readonly formKey = signal(0);

  protected readonly all = computed(() =>
    [...this.created(), ...(this.context.value()?.promotions ?? [])].map((promotion) => ({
      promotion,
      status: statusOf(promotion),
    })),
  );
  protected readonly options = computed<readonly ChipOption<PromotionStatus | 'ALL'>[]>(() => [
    { value: 'ALL', labelKey: 'promotions.filter.ALL', count: this.all().length },
    ...PROMOTION_STATUSES.map((value) => ({
      value,
      labelKey: `promotions.filter.${value}`,
      count: this.all().filter((item) => item.status === value).length,
    })),
  ]);
  protected readonly visible = computed(() =>
    this.all().filter((item) => this.status() === 'ALL' || item.status === this.status()),
  );

  /** Le serveur décide (422, 409) ; hors connexion, la saisie part en file et sera rejouée. */
  protected create(draft: PromotionDraft): void {
    const refs = this.context.value();
    if (!refs) {
      return;
    }
    this.promotionService.create(draft, refs).subscribe({
      next: (result) => {
        this.formKey.update((key) => key + 1);
        if (result.queued || !result.body) {
          this.toast.show('common.queuedSaved', {}, 'info');
          return;
        }
        const created = toCardView(result.body);
        this.created.update((all) => [created, ...all]);
        this.toast.show('promotions.scheduledToast', { name: created.name });
      },
      error: (error: unknown) =>
        this.toast.show('promotions.createFailed', { reason: errorMessage(error) }, 'danger'),
    });
  }
}
