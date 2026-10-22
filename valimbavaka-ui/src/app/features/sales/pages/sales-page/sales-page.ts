import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  linkedSignal,
  signal,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { Download, Plus } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ChannelFilter, ChannelSummary } from '../../components/channel-summary/channel-summary';
import { InvoicePanel } from '../../components/invoice-panel/invoice-panel';
import { INVOICE_TONES } from '../../components/invoice-tones';
import { SalesService } from '../../data-access/sales-service';

const PAGE_SIZE = 10;

/** Ventes du jour (VTE-07 à VTE-09) et facture de la vente sélectionnée (FAC-01 à FAC-05). */
@Component({
  selector: 'vvs-sales-page',
  imports: [
    RouterLink,
    TranslatePipe,
    AriaryPipe,
    Icon,
    PageHeader,
    ListState,
    Pagination,
    StatusBadge,
    ChannelSummary,
    InvoicePanel,
  ],
  templateUrl: './sales-page.html',
  styleUrl: './sales-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SalesPage {
  private readonly salesService = inject(SalesService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  /** Paramètre `?sale=` : lien partageable vers une facture. */
  readonly sale = input<string>();

  protected readonly icons = { Download, Plus };
  protected readonly tones = INVOICE_TONES;
  protected readonly pageSize = PAGE_SIZE;
  protected readonly day = rxResource({ stream: () => this.salesService.day() });
  protected readonly channel = signal<ChannelFilter>('ALL');
  protected readonly page = linkedSignal({ source: this.channel, computation: () => 1 });

  protected readonly filtered = computed(() => {
    const channel = this.channel();
    const sales = this.day.value()?.sales ?? [];
    return channel === 'ALL' ? sales : sales.filter((sale) => sale.channel === channel);
  });
  protected readonly rows = computed(() =>
    this.filtered().slice((this.page() - 1) * PAGE_SIZE, this.page() * PAGE_SIZE),
  );
  protected readonly selected = linkedSignal(
    () => this.sale() ?? this.day.value()?.sales.at(1)?.number ?? null,
  );

  protected select(number: string): void {
    this.selected.set(number);
    void this.router.navigate([], {
      queryParams: { sale: number },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  protected exportBook(): void {
    this.toast.show('common.demoAction', {}, 'info');
  }
}
