import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { ChevronLeft, Info, TriangleAlert } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language-service';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ShortDatePipe } from '../../../../shared/pipes/short-date-pipe';
import { LOT_STATUS_TONES } from '../../data-access/lot-tones';
import { ProductionService } from '../../data-access/production-service';

/**
 * Détail d'un lot (PRD-05) : entrants, coût matière et coût de revient unitaire calculés par le serveur à partir des
 * entrants (coûts figés à la validation, estimés avant), marge sur le prix de vente.
 */
@Component({
  selector: 'vvs-production-lot-page',
  imports: [RouterLink, TranslatePipe, AriaryPipe, ShortDatePipe, Icon, ListState, StatusBadge],
  templateUrl: './production-lot-page.html',
  styleUrl: './production-lot-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductionLotPage {
  private readonly productionService = inject(ProductionService);
  private readonly language = inject(LanguageService);

  /** Paramètre de route `:id`. */
  readonly id = input.required<string>();

  protected readonly icons = { ChevronLeft, Info, TriangleAlert };
  protected readonly lotTones = LOT_STATUS_TONES;
  protected readonly lot = rxResource({
    params: () => this.id(),
    stream: ({ params }) => this.productionService.cost(params),
  });
  protected readonly numberFormat = computed(
    () => new Intl.NumberFormat(this.language.current(), { maximumFractionDigits: 3 }),
  );
}
