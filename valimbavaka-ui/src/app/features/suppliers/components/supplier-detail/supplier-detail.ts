import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Pencil } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { AvatarInitials } from '../../../../shared/components/avatar-initials/avatar-initials';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { TabItem, Tabs } from '../../../../shared/components/tabs/tabs';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ShortDatePipe } from '../../../../shared/pipes/short-date-pipe';
import { SupplierService } from '../../data-access/supplier-service';
import { SUPPLIER_TYPE_TONES } from '../../data-access/supplier-tones';

type DetailTab = 'info' | 'articles' | 'receptions';

/** Fiche fournisseur (FRS-02 à FRS-04) : coordonnées, articles fournis, réceptions. */
@Component({
  selector: 'vvs-supplier-detail',
  imports: [
    TranslatePipe,
    AriaryPipe,
    ShortDatePipe,
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

  readonly supplierId = input.required<string>();
  readonly edit = output<void>();

  protected readonly icons = { Pencil };
  protected readonly typeTones = SUPPLIER_TYPE_TONES;
  protected readonly tab = signal<DetailTab>('info');
  protected readonly tabs: readonly TabItem<DetailTab>[] = [
    { id: 'info', labelKey: 'suppliers.tab.info' },
    { id: 'articles', labelKey: 'suppliers.tab.articles' },
    { id: 'receptions', labelKey: 'suppliers.tab.receptions' },
  ];
  protected readonly supplier = rxResource({
    params: () => this.supplierId(),
    stream: ({ params }) => this.supplierService.get(params),
  });
}
