import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { SalonServiceRDto } from '../../data-access/salon-catalog-models';
import { SalonCatalogService } from '../../data-access/salon-catalog-service';

const PAGE_SIZE = 10;

/**
 * Catalogue des services salon (SRV-01) : recherche, pagination serveur, activation et désactivation confirmées.
 * Lecture pour la vendeuse ; modification et activation pour l'administrateur. Un service ne se supprime pas.
 */
@Component({
  selector: 'vvs-salon-catalog-list-page',
  imports: [
    RouterLink,
    HasPermissionDirective,
    TranslatePipe,
    AriaryPipe,
    ConfirmDialog,
    Icon,
    ListState,
    PageHeader,
    Pagination,
    SearchField,
    StatusBadge,
  ],
  templateUrl: './salon-catalog-list-page.html',
  styleUrl: './salon-catalog-list-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SalonCatalogListPage {
  private readonly catalogService = inject(SalonCatalogService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly translate = inject(TranslateService);

  protected readonly icons = { Plus };
  protected readonly pageSize = PAGE_SIZE;
  protected readonly services = signal<SalonServiceRDto[]>([]);
  /** Page courante en base 1, comme le contrat de l'API. */
  protected readonly page = signal(1);
  protected readonly totalCount = signal(0);
  protected readonly search = signal('');
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  /** Service dont le changement de statut attend la confirmation. */
  protected readonly pending = signal<SalonServiceRDto | null>(null);

  constructor() {
    this.loadServices();
  }

  protected onSearch(term: string): void {
    this.search.set(term.trim());
    this.page.set(1);
    this.loadServices();
  }

  protected goToPage(page: number): void {
    this.page.set(page);
    this.loadServices();
  }

  protected ask(service: SalonServiceRDto): void {
    this.pending.set(service);
  }

  protected dismiss(): void {
    this.pending.set(null);
  }

  /** Textes du dialogue de confirmation selon le statut actuel du service. */
  protected dialogTexts(service: SalonServiceRDto): {
    heading: string;
    message: string;
    confirm: string;
  } {
    const key = service.active ? 'deactivate' : 'activate';
    return {
      heading: this.translate.instant(`salonCatalog.confirm.${key}Title`),
      message: this.translate.instant(`salonCatalog.confirm.${key}Message`, {
        name: service.name,
      }),
      confirm: this.translate.instant(`salonCatalog.status.${key}`),
    };
  }

  protected confirm(): void {
    const service = this.pending();
    this.pending.set(null);
    if (!service) {
      return;
    }
    this.catalogService
      .setActive(service.id, !service.active)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          if (result.queued) {
            this.toast.show('common.queuedSaved', {}, 'info');
          }
          this.loadServices();
        },
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  protected loadServices(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.catalogService
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
          this.services.set(result.items);
          this.totalCount.set(result.totalCount);
        },
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }
}
