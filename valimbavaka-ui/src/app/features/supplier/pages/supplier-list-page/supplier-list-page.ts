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
import { TranslatePipe } from '@ngx-translate/core';
import { Subscription, finalize } from 'rxjs';
import { HasPermissionDirective } from '../../../../core/auth/has-permission-directive';
import { errorMessage } from '../../../../core/http/problem-detail';
import { SupplierRDto, SupplierType } from '../../data-access/supplier-models';
import { SupplierService } from '../../data-access/supplier-service';

const PAGE_SIZE = 20;

/** Liste des fournisseurs (FRS-01) : filtre par type et recherche (FRS-03), pagination, activation. */
@Component({
  selector: 'vvs-supplier-list-page',
  imports: [RouterLink, HasPermissionDirective, TranslatePipe],
  templateUrl: './supplier-list-page.html',
  styleUrl: './supplier-list-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SupplierListPage {
  private readonly supplierService = inject(SupplierService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly suppliers = signal<SupplierRDto[]>([]);
  /** Page courante en base 1, comme le contrat de l'API. */
  protected readonly page = signal(1);
  protected readonly totalPages = signal(0);
  protected readonly types: readonly SupplierType[] = ['LOCAL', 'FOREIGN'];
  /** Type choisi ; vide = tous les types. */
  protected readonly type = signal<SupplierType | ''>('');
  /** Recherche sur le nom du fournisseur et celui du contact. */
  protected readonly search = signal('');
  protected readonly isFiltered = computed(() => !!this.type() || !!this.search());
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  /** Requête de liste en cours : annulée si un filtre change avant sa réponse. */
  private listRequest?: Subscription;

  constructor() {
    this.loadSuppliers();
  }

  protected onTypeChange(type: string): void {
    this.type.set(type === 'LOCAL' || type === 'FOREIGN' ? type : '');
    this.reload();
  }

  protected onSearch(term: string): void {
    this.search.set(term.trim());
    this.reload();
  }

  protected goToPage(page: number): void {
    this.page.set(page);
    this.loadSuppliers();
  }

  protected toggleActive(supplier: SupplierRDto): void {
    this.errorMessage.set(null);
    this.supplierService
      .setActive(supplier.id, !supplier.active)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) =>
          this.suppliers.update((list) => list.map((s) => (s.id === updated.id ? updated : s))),
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  /** Un nouveau filtre repart de la première page. */
  private reload(): void {
    this.page.set(1);
    this.loadSuppliers();
  }

  private loadSuppliers(): void {
    this.listRequest?.unsubscribe();
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.listRequest = this.supplierService
      .list({
        page: this.page(),
        size: PAGE_SIZE,
        sortBy: 'name',
        sortDirection: 'ASC',
        search: this.search(),
        type: this.type() || undefined,
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false)),
      )
      .subscribe({
        next: (result) => {
          this.suppliers.set(result.items);
          this.totalPages.set(result.totalPages);
        },
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }
}
