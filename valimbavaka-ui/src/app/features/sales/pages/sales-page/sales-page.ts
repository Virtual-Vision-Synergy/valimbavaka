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
import { ChartColumn, Download, Plus, Receipt } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthStore } from '../../../../core/auth/auth-store';
import { ReferentialService } from '../../../referential/data-access/referential-service';
import { ChipOption, FilterChips } from '../../../../shared/components/filter-chips/filter-chips';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { Tone } from '../../../../shared/models/tone';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { InvoicePanel } from '../../components/invoice-panel/invoice-panel';
import { SalesBookExport } from '../../components/sales-book-export/sales-book-export';
import { localIsoDate, toSaleRow } from '../../data-access/sales-mapping';
import { SaleStatus } from '../../data-access/sales-models';
import { SalesService } from '../../data-access/sales-service';

const PAGE_SIZE = 10;
const ALL_CHANNELS = 'ALL';

/** Tonalité du badge d'une vente annulée ou contre-passée (une vente validée n'en affiche pas). */
const STATUS_TONES: Readonly<Record<SaleStatus, Tone>> = {
  VALIDATED: 'success',
  CANCELLED: 'muted',
  REVERSED: 'danger',
};

/**
 * Ventes du jour (VTE-07) : liste paginée par le serveur, filtrée par jour et par canal, et facture de la vente
 * sélectionnée. Une vendeuse ne voit que ses ventes (filtre imposé par le serveur), l'administrateur toutes.
 */
@Component({
  selector: 'vvs-sales-page',
  imports: [
    RouterLink,
    TranslatePipe,
    AriaryPipe,
    Icon,
    PageHeader,
    FilterChips,
    ListState,
    Pagination,
    StatusBadge,
    InvoicePanel,
    SalesBookExport,
  ],
  templateUrl: './sales-page.html',
  styleUrl: './sales-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SalesPage {
  private readonly salesService = inject(SalesService);
  private readonly referentialService = inject(ReferentialService);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthStore);
  protected readonly canSeeChannels = this.auth.hasPermission('reports.read');
  /** Le livre des ventes est réservé à l'administrateur (`invoices.export`) ; le serveur décide. */
  protected readonly canExportBook = this.auth.hasPermission('invoices.export');
  protected readonly bookOpen = signal(false);

  /** Paramètre `?sale=` : lien partageable vers la facture d'une vente de la page. */
  readonly sale = input<string>();

  protected readonly icons = { ChartColumn, Download, Plus, Receipt };
  protected readonly tones = STATUS_TONES;
  protected readonly pageSize = PAGE_SIZE;
  protected readonly date = signal(localIsoDate(new Date()));
  protected readonly channel = signal(ALL_CHANNELS);
  protected readonly page = linkedSignal({
    source: () => [this.date(), this.channel()],
    computation: () => 1,
  });

  private readonly channels = rxResource({
    stream: () => this.referentialService.listSalesChannels(),
  });
  protected readonly channelOptions = computed<readonly ChipOption<string>[]>(() => [
    { value: ALL_CHANNELS, labelKey: 'sales.channel.ALL' },
    ...(this.channels.value() ?? []).map((channel) => ({
      value: channel.id,
      label: channel.label,
    })),
  ]);

  protected readonly sales = rxResource({
    params: () => ({
      page: this.page(),
      size: PAGE_SIZE,
      date: this.date() || undefined,
      salesChannelId: this.channel() === ALL_CHANNELS ? undefined : this.channel(),
    }),
    stream: ({ params }) => this.salesService.list(params),
  });
  protected readonly rows = computed(() => (this.sales.value()?.items ?? []).map(toSaleRow));
  protected readonly selected = linkedSignal(() => this.sale() ?? null);
  protected readonly selectedSale = computed(() =>
    this.sales.value()?.items.find((sale) => sale.number === this.selected()),
  );

  protected onDateChange(event: Event): void {
    this.date.set((event.target as HTMLInputElement).value);
  }

  protected select(number: string): void {
    this.selected.set(number);
    void this.router.navigate([], {
      queryParams: { sale: number },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  protected toggleBook(): void {
    this.bookOpen.update((open) => !open);
  }
}
