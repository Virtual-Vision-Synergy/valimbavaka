import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { LanguageService } from '../../../../core/i18n/language-service';
import { errorMessage } from '../../../../core/http/problem-detail';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ShortDatePipe } from '../../../../shared/pipes/short-date-pipe';
import { CommissionRDto } from '../../data-access/commission-models';
import { CommissionService } from '../../data-access/commission-service';

const PAGE_SIZE = 20;
/** Mois proposés : le mois en cours et les 11 précédents. */
const MONTH_COUNT = 12;

/** Clé `AAAA-MM` du mois décalé de `offset` par rapport à `today`. */
export function monthKey(today: Date, offset: number): string {
  const day = new Date(today.getFullYear(), today.getMonth() + offset, 1);
  return `${day.getFullYear()}-${String(day.getMonth() + 1).padStart(2, '0')}`;
}

/** Première et dernière journée (`AAAA-MM-JJ`) du mois `AAAA-MM`. */
export function monthRange(month: string): { from: string; to: string } {
  const [year, monthNumber] = month.split('-').map(Number);
  const lastDay = new Date(year, monthNumber, 0).getDate();
  return { from: `${month}-01`, to: `${month}-${String(lastDay).padStart(2, '0')}` };
}

/**
 * Commissions calculées (COM-02), toutes vendeuses, pour l'administrateur : chaque vente ou prestation créditée à une
 * vendeuse, avec la base (CA ou marge), le taux appliqué et son origine (service salon, canal ou vendeuse), et la
 * commission = base × taux. Le calcul est fait par le serveur à l'enregistrement de l'opération.
 */
@Component({
  selector: 'vvs-commission-list-page',
  imports: [TranslatePipe, AriaryPipe, ShortDatePipe, ListState, PageHeader, Pagination],
  templateUrl: './commission-list-page.html',
  styleUrl: './commission-list-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CommissionListPage {
  private readonly commissionService = inject(CommissionService);
  private readonly language = inject(LanguageService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly pageSize = PAGE_SIZE;
  protected readonly commissions = signal<CommissionRDto[]>([]);
  /** Page courante en base 1, comme le contrat de l'API. */
  protected readonly page = signal(1);
  protected readonly totalCount = signal(0);
  protected readonly month = signal(monthKey(new Date(), 0));
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly months = computed(() => {
    const today = new Date();
    return Array.from({ length: MONTH_COUNT }, (_, index) => {
      const value = monthKey(today, -index);
      return { value, label: this.monthLabel(value) };
    });
  });

  constructor() {
    this.loadCommissions();
  }

  protected selectMonth(value: string): void {
    this.month.set(value);
    this.page.set(1);
    this.loadCommissions();
  }

  protected goToPage(page: number): void {
    this.page.set(page);
    this.loadCommissions();
  }

  protected loadCommissions(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.commissionService
      .list({
        page: this.page(),
        size: PAGE_SIZE,
        sortBy: 'createdAt',
        sortDirection: 'DESC',
        ...monthRange(this.month()),
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false)),
      )
      .subscribe({
        next: (result) => {
          this.commissions.set(result.items);
          this.totalCount.set(result.totalCount);
        },
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  private monthLabel(value: string): string {
    const label = new Intl.DateTimeFormat(this.language.current(), {
      month: 'long',
      year: 'numeric',
    }).format(new Date(`${value}-01T12:00:00`));
    return label.charAt(0).toUpperCase() + label.slice(1);
  }
}
