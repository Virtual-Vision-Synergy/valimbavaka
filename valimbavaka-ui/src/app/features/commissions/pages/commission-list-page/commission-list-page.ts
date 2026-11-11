import { HttpParams } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Download, RefreshCw } from 'lucide';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { AuthStore } from '../../../../core/auth/auth-store';
import { LanguageService } from '../../../../core/i18n/language-service';
import { errorMessage } from '../../../../core/http/problem-detail';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { ExportFormat, ExportService } from '../../../../shared/data-access/export-service';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ShortDatePipe } from '../../../../shared/pipes/short-date-pipe';
import {
  CommissionRDto,
  CommissionRecalculationRDto,
  CommissionValidationRDto,
  SellerCommissionRDto,
} from '../../data-access/commission-models';
import { CommissionService } from '../../data-access/commission-service';

const COMMISSIONS_URL = '/api/v1/commissions';
const PAGE_SIZE = 20;
/** Vendeuses affichées en une page : le maximum accepté par l'API. */
const SELLER_PAGE_SIZE = 100;
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
 * commission = base × taux. Le calcul est fait par le serveur à l'enregistrement de l'opération. Au-dessus, le statut
 * à valider / validée de chaque vendeuse sur le mois, et la validation de ses commissions (COM-03) après confirmation :
 * le serveur décide (`commissions.validate`), une commission validée est définitive. COM-05 : l'administrateur
 * recalcule les commissions non validées du mois affiché après confirmation (`commissions.recalculate` ; une validée
 * ou une annulée n'est jamais touchée) et exporte le mois en Excel ou en PDF ; les deux actions confirment leur
 * exécution (message du recalcul, notification de l'export).
 */
@Component({
  selector: 'vvs-commission-list-page',
  imports: [
    TranslatePipe,
    AriaryPipe,
    ShortDatePipe,
    ListState,
    PageHeader,
    Pagination,
    ConfirmDialog,
    Icon,
    RouterLink,
  ],
  templateUrl: './commission-list-page.html',
  styleUrl: './commission-list-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CommissionListPage {
  private readonly commissionService = inject(CommissionService);
  private readonly language = inject(LanguageService);
  private readonly translate = inject(TranslateService);
  private readonly exportService = inject(ExportService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly icons = { Download, RefreshCw };
  protected readonly pageSize = PAGE_SIZE;
  protected readonly commissions = signal<CommissionRDto[]>([]);
  /** Page courante en base 1, comme le contrat de l'API. */
  protected readonly page = signal(1);
  protected readonly totalCount = signal(0);
  protected readonly month = signal(monthKey(new Date(), 0));
  protected readonly canValidate = inject(AuthStore).hasPermission('commissions.validate');
  protected readonly canRecalculate = inject(AuthStore).hasPermission('commissions.recalculate');
  protected readonly isRecalculationPending = signal(false);
  protected readonly isRecalculating = signal(false);
  protected readonly isExporting = signal(false);
  protected readonly sellers = signal<SellerCommissionRDto[]>([]);
  protected readonly sellersError = signal<string | null>(null);
  /** Vendeuse dont la validation attend la confirmation. */
  protected readonly pending = signal<SellerCommissionRDto | null>(null);
  protected readonly isValidating = signal(false);
  protected readonly notice = signal<string | null>(null);
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
    this.loadSellers();
  }

  protected selectMonth(value: string): void {
    this.month.set(value);
    this.page.set(1);
    this.notice.set(null);
    this.loadCommissions();
    this.loadSellers();
  }

  protected askRecalculation(): void {
    this.isRecalculationPending.set(true);
  }

  protected dismissRecalculation(): void {
    this.isRecalculationPending.set(false);
  }

  /** Recalcule le mois affiché, toutes vendeuses ; le résultat chiffré confirme l'exécution. */
  protected confirmRecalculation(): void {
    this.isRecalculationPending.set(false);
    this.isRecalculating.set(true);
    this.sellersError.set(null);
    this.notice.set(null);
    this.commissionService
      .recalculate(monthRange(this.month()))
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isRecalculating.set(false)),
      )
      .subscribe({
        next: (result) => this.onRecalculated(result),
        error: (error: unknown) => this.sellersError.set(errorMessage(error)),
      });
  }

  /** Télécharge le mois affiché, toutes vendeuses ; le service d'export confirme la génération du fichier. */
  protected exportAs(format: ExportFormat): void {
    const { from, to } = monthRange(this.month());
    this.isExporting.set(true);
    this.exportService
      .download(COMMISSIONS_URL, format, new HttpParams().set('from', from).set('to', to))
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isExporting.set(false)),
      )
      .subscribe({ error: () => undefined });
  }

  protected askValidation(seller: SellerCommissionRDto): void {
    this.pending.set(seller);
  }

  protected dismiss(): void {
    this.pending.set(null);
  }

  /** Textes du dialogue de confirmation de la validation. */
  protected dialogTexts(seller: SellerCommissionRDto): { message: string } {
    return {
      message: this.translate.instant('commissionList.validate.confirmMessage', {
        name: seller.sellerName,
        count: seller.toValidateCount,
        amount: seller.toValidateAmount,
      }),
    };
  }

  protected confirmValidation(): void {
    const seller = this.pending();
    this.pending.set(null);
    if (!seller) {
      return;
    }
    this.isValidating.set(true);
    this.sellersError.set(null);
    this.notice.set(null);
    this.commissionService
      .validate({ sellerId: seller.sellerId, ...monthRange(this.month()) })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isValidating.set(false)),
      )
      .subscribe({
        next: (result) => this.onValidated(result),
        error: (error: unknown) => {
          this.sellersError.set(errorMessage(error));
          this.loadSellers();
        },
      });
  }

  private onRecalculated(result: CommissionRecalculationRDto): void {
    const params = {
      updated: result.updatedCount,
      unchanged: result.unchangedCount,
      validated: result.validatedCount,
    };
    this.notice.set(this.translate.instant('commissionList.recalculate.done', params));
    this.loadSellers();
    this.loadCommissions();
  }

  private onValidated(result: CommissionValidationRDto): void {
    const params = { name: result.sellerName, count: result.validatedCount };
    this.notice.set(this.translate.instant('commissionList.validate.done', params));
    this.loadSellers();
    this.loadCommissions();
  }

  protected loadSellers(): void {
    const { from, to } = monthRange(this.month());
    this.commissionService
      .sellers({ page: 1, size: SELLER_PAGE_SIZE }, from, to)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => this.sellers.set(result.items),
        error: (error: unknown) => this.sellersError.set(errorMessage(error)),
      });
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
