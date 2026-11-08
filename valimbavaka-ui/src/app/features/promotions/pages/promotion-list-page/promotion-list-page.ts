import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  linkedSignal,
  signal,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { errorMessage } from '../../../../core/http/problem-detail';
import { ChipOption, FilterChips } from '../../../../shared/components/filter-chips/filter-chips';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { SidePanel, isPanelDocked } from '../../../../shared/components/side-panel/side-panel';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { PromotionCard } from '../../components/promotion-card/promotion-card';
import { PromotionDetail } from '../../components/promotion-detail/promotion-detail';
import { PromotionForm } from '../../components/promotion-form/promotion-form';
import {
  PROMOTION_STATUSES,
  PromotionDraft,
  PromotionStatus,
} from '../../data-access/promotion-models';
import { toCardView } from '../../data-access/promotion-request';
import { PromotionService } from '../../data-access/promotion-service';

export const PROMOTION_PAGE_SIZE = 12;
const STATUS_OPTIONS: readonly ChipOption<PromotionStatus | 'ALL'>[] = [
  { value: 'ALL', labelKey: 'promotions.filter.ALL' },
  ...PROMOTION_STATUSES.map((value) => ({ value, labelKey: `promotions.filter.${value}` })),
];

/**
 * Promotions (PRO-01, PRO-04) : liste paginée et filtrée par statut côté serveur, fiche détaillée au clic
 * sur une carte, formulaire de création à droite.
 */
@Component({
  selector: 'vvs-promotion-list-page',
  imports: [
    TranslatePipe,
    PageHeader,
    ListState,
    FilterChips,
    Pagination,
    SidePanel,
    PromotionCard,
    PromotionDetail,
    PromotionForm,
  ],
  templateUrl: './promotion-list-page.html',
  styleUrl: './promotion-list-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromotionListPage {
  private readonly promotionService = inject(PromotionService);
  private readonly toast = inject(ToastService);

  /** Ancré (très grand écran), le panneau prend la place du formulaire ; sinon c'est un tiroir. */
  protected readonly docked = isPanelDocked();
  protected readonly pageSize = PROMOTION_PAGE_SIZE;
  protected readonly statusOptions = STATUS_OPTIONS;
  protected readonly context = rxResource({ stream: () => this.promotionService.context() });
  protected readonly status = signal<PromotionStatus | 'ALL'>('ALL');
  protected readonly page = linkedSignal({ source: this.status, computation: () => 1 });
  protected readonly formKey = signal(0);
  protected readonly selectedId = signal<string | null>(null);

  /** Un nouveau filtre ou une nouvelle page annule la requête précédente. */
  protected readonly promotions = rxResource({
    params: () => ({ page: this.page(), status: this.status() }),
    stream: ({ params }) =>
      this.promotionService.list({
        page: params.page,
        size: PROMOTION_PAGE_SIZE,
        status: params.status === 'ALL' ? undefined : params.status,
      }),
  });
  /** `value()` lève une exception tant que la ressource est en erreur : toujours passer par `hasValue()`. */
  protected readonly items = computed(() =>
    this.promotions.hasValue()
      ? this.promotions.value().items.map((promotion) => ({
          promotion: toCardView(promotion),
          status: promotion.status,
        }))
      : [],
  );
  protected readonly selectedName = computed(
    () =>
      this.items().find((item) => item.promotion.id === this.selectedId())?.promotion.name ?? '',
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
        this.toast.show('promotions.scheduledToast', { name: result.body.name });
        this.promotions.reload();
      },
      error: (error: unknown) =>
        this.toast.show('promotions.createFailed', { reason: errorMessage(error) }, 'danger'),
    });
  }
}
