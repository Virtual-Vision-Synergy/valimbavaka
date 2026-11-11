import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Download } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthStore } from '../../../../core/auth/auth-store';
import { LanguageService } from '../../../../core/i18n/language-service';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ShortDatePipe } from '../../../../shared/pipes/short-date-pipe';
import { Tone } from '../../../../shared/models/tone';
import { CommissionStatus } from '../../data-access/commission-models';
import { daysLeftInMonth, monthKey } from '../../data-access/commission-months';
import { CommissionService } from '../../data-access/commission-service';

/** Circonférence du cercle de progression (rayon 52). */
const RING_CIRCUMFERENCE = 2 * Math.PI * 52;
const MONTH_COUNT = 12;
const PAGE_SIZE = 20;
const ROUND_TO = 1000;

const STATUS_TONES: Readonly<Record<CommissionStatus, Tone>> = {
  CALCULATED: 'warning',
  VALIDATED: 'success',
  CANCELLED: 'muted',
};

/**
 * Mes commissions (COM-04) : commission du mois, avancement vers l'objectif mensuel de la fiche utilisateur et détail
 * par vente. Une vendeuse voit les siennes (le serveur impose son identifiant) ; l'administrateur arrive depuis
 * « Commissions calculées » avec `?sellerId=` et choisit le mois.
 */
@Component({
  selector: 'vvs-commissions-page',
  imports: [
    TranslatePipe,
    AriaryPipe,
    ShortDatePipe,
    Icon,
    ListState,
    PageHeader,
    Pagination,
    StatusBadge,
  ],
  templateUrl: './commissions-page.html',
  styleUrl: './commissions-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CommissionsPage {
  private readonly commissionService = inject(CommissionService);
  private readonly toast = inject(ToastService);
  private readonly language = inject(LanguageService);
  private readonly isAdministrator = inject(AuthStore).hasPermission('commissions.read-all');

  /** Paramètres d'URL (liaison des entrées du routeur) : vendeuse et mois choisis par l'administrateur. */
  readonly sellerId = input<string | undefined>();
  readonly month = input<string | undefined>();

  protected readonly icons = { Download };
  protected readonly tones = STATUS_TONES;
  protected readonly circumference = RING_CIRCUMFERENCE;
  protected readonly pageSize = PAGE_SIZE;
  protected readonly page = signal(1);
  private readonly chosenMonth = signal<string | null>(null);
  protected readonly selectedMonth = computed(
    () => this.chosenMonth() ?? this.month() ?? monthKey(new Date(), 0),
  );
  /** L'administrateur doit désigner une vendeuse ; pour une vendeuse, le serveur ignore ce paramètre. */
  protected readonly needsSeller = computed(() => this.isAdministrator && !this.sellerId());

  protected readonly followUp = rxResource({
    params: () =>
      this.needsSeller()
        ? undefined
        : { month: this.selectedMonth(), sellerId: this.requestedSeller() },
    stream: ({ params }) => this.commissionService.followUp(params.month, params.sellerId),
  });
  protected readonly lines = rxResource({
    params: () =>
      this.needsSeller()
        ? undefined
        : { month: this.selectedMonth(), sellerId: this.requestedSeller(), page: this.page() },
    stream: ({ params }) =>
      this.commissionService.followUpLines(
        { page: params.page, size: PAGE_SIZE },
        params.month,
        params.sellerId,
      ),
  });

  protected readonly months = computed(() => {
    const today = new Date();
    const values = Array.from({ length: MONTH_COUNT }, (_, index) => monthKey(today, -index));
    const chosen = this.selectedMonth();
    return (values.includes(chosen) ? values : [chosen, ...values]).map((value) => ({
      value,
      label: this.monthLabel(value),
    }));
  });
  /** Part de l'objectif atteinte, en pourcentage entier ; peut dépasser 100. */
  protected readonly percent = computed(() => {
    const value = this.followUp.value()?.targetProgressPercent;
    return value === null || value === undefined ? null : Math.round(value);
  });
  protected readonly ringPercent = computed(() => Math.min(100, this.percent() ?? 0));
  protected readonly remaining = computed(() => {
    const data = this.followUp.value();
    const days = daysLeftInMonth(this.selectedMonth(), new Date());
    if (!data || data.monthlyTarget === null || days === 0) {
      return null;
    }
    const left = Math.max(0, data.monthlyTarget - data.revenue);
    return left === 0
      ? null
      : { left, days, perDay: Math.round(left / days / ROUND_TO) * ROUND_TO };
  });

  protected selectMonth(value: string): void {
    this.chosenMonth.set(value);
    this.page.set(1);
  }

  protected goToPage(page: number): void {
    this.page.set(page);
  }

  protected exportStatement(): void {
    this.toast.show('common.demoAction', {}, 'info');
  }

  protected monthLabel(value: string): string {
    const label = new Intl.DateTimeFormat(this.language.current(), {
      month: 'long',
      year: 'numeric',
    }).format(new Date(`${value}-01T12:00:00`));
    return label.charAt(0).toUpperCase() + label.slice(1);
  }

  /** Une vendeuse n'envoie jamais de vendeuse : le serveur impose la sienne. */
  private requestedSeller(): string | undefined {
    return this.isAdministrator ? this.sellerId() : undefined;
  }
}
