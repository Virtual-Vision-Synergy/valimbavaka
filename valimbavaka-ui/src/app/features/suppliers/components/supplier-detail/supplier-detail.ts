import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Pencil } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { HasPermissionDirective } from '../../../../core/auth/has-permission-directive';
import { errorMessage } from '../../../../core/http/problem-detail';
import { AvatarInitials } from '../../../../shared/components/avatar-initials/avatar-initials';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { TabItem, Tabs } from '../../../../shared/components/tabs/tabs';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ArticleService } from '../../../article/data-access/article-service';
import { SupplierRDto } from '../../../supplier/data-access/supplier-models';
import { SupplierService } from '../../../supplier/data-access/supplier-service';
import { SUPPLIER_TYPE_TONES } from '../../data-access/supplier-tones';

type DetailTab = 'info' | 'articles';

/** Une page de 100 articles suffit pour la fiche (au-delà, la liste des articles prend le relais). */
const ARTICLES_PAGE = { page: 1, size: 100, sortBy: 'name', sortDirection: 'ASC' } as const;

/** Fiche fournisseur (FRS-01, FRS-02) : coordonnées, articles fournis, activation, modification. */
@Component({
  selector: 'vvs-supplier-detail',
  imports: [
    RouterLink,
    HasPermissionDirective,
    TranslatePipe,
    AriaryPipe,
    Icon,
    AvatarInitials,
    ListState,
    StatusBadge,
    Tabs,
  ],
  templateUrl: './supplier-detail.html',
  styleUrl: './supplier-detail.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SupplierDetail {
  private readonly supplierService = inject(SupplierService);
  private readonly articleService = inject(ArticleService);
  private readonly destroyRef = inject(DestroyRef);

  readonly supplierId = input.required<string>();
  /** Émis après un changement d'état, pour que la liste se recharge. */
  readonly changed = output<SupplierRDto>();

  protected readonly icons = { Pencil };
  protected readonly typeTones = SUPPLIER_TYPE_TONES;
  protected readonly tab = signal<DetailTab>('info');
  protected readonly tabs: readonly TabItem<DetailTab>[] = [
    { id: 'info', labelKey: 'suppliers.tab.info' },
    { id: 'articles', labelKey: 'suppliers.tab.articles' },
  ];
  protected readonly isToggling = signal(false);
  protected readonly toggleError = signal<string | null>(null);
  protected readonly supplier = rxResource({
    params: () => this.supplierId(),
    stream: ({ params }) => this.supplierService.getById(params),
  });
  /** Les articles ne sont lus qu'à l'ouverture de l'onglet. */
  protected readonly articles = rxResource({
    params: () => (this.tab() === 'articles' ? this.supplierId() : undefined),
    stream: ({ params }) => this.articleService.list({ ...ARTICLES_PAGE, supplierId: params }),
  });

  protected toggleActive(supplier: SupplierRDto): void {
    this.isToggling.set(true);
    this.toggleError.set(null);
    this.supplierService
      .setActive(supplier.id, !supplier.active)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updated) => {
          this.isToggling.set(false);
          this.supplier.set(updated);
          this.changed.emit(updated);
        },
        error: (error: unknown) => {
          this.isToggling.set(false);
          this.toggleError.set(errorMessage(error));
        },
      });
  }
}
