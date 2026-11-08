import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Plus } from 'lucide';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { HasPermissionDirective } from '../../../../core/auth/has-permission-directive';
import { errorMessage } from '../../../../core/http/problem-detail';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { SearchField } from '../../../../shared/components/search-field/search-field';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { ReferentialService } from '../../../referential/data-access/referential-service';
import { CommissionRuleRDto } from '../../data-access/commission-rule-models';
import { CommissionRuleService } from '../../data-access/commission-rule-service';

const PAGE_SIZE = 10;

/**
 * Règles de commission par canal (COM-01) : recherche, pagination serveur, activation et désactivation confirmées.
 * Une règle ne se supprime pas (JNL-02) : elle se désactive, et un canal n'a qu'une règle active à la fois.
 */
@Component({
  selector: 'vvs-commission-rule-list-page',
  imports: [
    RouterLink,
    HasPermissionDirective,
    TranslatePipe,
    ConfirmDialog,
    Icon,
    ListState,
    PageHeader,
    Pagination,
    SearchField,
    StatusBadge,
  ],
  templateUrl: './commission-rule-list-page.html',
  styleUrl: './commission-rule-list-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CommissionRuleListPage {
  private readonly ruleService = inject(CommissionRuleService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly translate = inject(TranslateService);

  protected readonly icons = { Plus };
  protected readonly pageSize = PAGE_SIZE;
  protected readonly rules = signal<CommissionRuleRDto[]>([]);
  /** Page courante en base 1, comme le contrat de l'API. */
  protected readonly page = signal(1);
  protected readonly totalCount = signal(0);
  protected readonly search = signal('');
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  /** Règle dont le changement de statut attend la confirmation. */
  protected readonly pending = signal<CommissionRuleRDto | null>(null);
  private readonly salesChannels = toSignal(inject(ReferentialService).listSalesChannels(), {
    initialValue: [],
  });
  private readonly channelLabels = computed(
    () => new Map(this.salesChannels().map((channel) => [channel.id, channel.label])),
  );

  constructor() {
    this.loadRules();
  }

  /** Libellé du canal ; un canal désactivé depuis n'est plus listé par le référentiel. */
  protected channelLabel(rule: CommissionRuleRDto): string {
    return (
      this.channelLabels().get(rule.salesChannelId) ??
      this.translate.instant('commissionRule.unknownChannel')
    );
  }

  protected onSearch(term: string): void {
    this.search.set(term.trim());
    this.page.set(1);
    this.loadRules();
  }

  protected goToPage(page: number): void {
    this.page.set(page);
    this.loadRules();
  }

  protected ask(rule: CommissionRuleRDto): void {
    this.pending.set(rule);
  }

  protected dismiss(): void {
    this.pending.set(null);
  }

  /** Textes du dialogue de confirmation selon le statut actuel de la règle. */
  protected dialogTexts(rule: CommissionRuleRDto): {
    heading: string;
    message: string;
    confirm: string;
  } {
    const key = rule.active ? 'deactivate' : 'activate';
    return {
      heading: this.translate.instant(`commissionRule.confirm.${key}Title`),
      message: this.translate.instant(`commissionRule.confirm.${key}Message`, { name: rule.name }),
      confirm: this.translate.instant(`commissionRule.status.${key}`),
    };
  }

  protected confirm(): void {
    const rule = this.pending();
    this.pending.set(null);
    if (!rule) {
      return;
    }
    this.ruleService
      .setActive(rule.id, !rule.active)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          if (result.queued) {
            this.toast.show('common.queuedSaved', {}, 'info');
          }
          this.loadRules();
        },
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  protected loadRules(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.ruleService
      .list({
        search: this.search(),
        page: this.page(),
        size: PAGE_SIZE,
        sortBy: 'name',
        sortDirection: 'ASC',
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false)),
      )
      .subscribe({
        next: (result) => {
          this.rules.set(result.items);
          this.totalCount.set(result.totalCount);
        },
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }
}
