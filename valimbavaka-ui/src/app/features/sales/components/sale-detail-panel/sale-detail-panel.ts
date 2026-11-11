import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { Tone } from '../../../../shared/models/tone';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { CommissionStatus } from '../../../commissions/data-access/commission-models';
import { localDateTime } from '../../data-access/sales-mapping';
import { SaleStatus } from '../../data-access/sales-models';
import { SalesService } from '../../data-access/sales-service';

/** Tonalité du badge d'une vente (une vente validée est la normale, les autres se signalent). */
const SALE_TONES: Readonly<Record<SaleStatus, Tone>> = {
  VALIDATED: 'success',
  CANCELLED: 'muted',
  REVERSED: 'danger',
};

/** Tonalité du badge d'une commission, toujours doublée de son libellé. */
const COMMISSION_TONES: Readonly<Record<CommissionStatus, Tone>> = {
  CALCULATED: 'warning',
  VALIDATED: 'success',
  CANCELLED: 'muted',
};

/**
 * Détail de la vente sélectionnée (VTE-09) : lignes avec prix unitaires, promotion, numéro de facture et commission
 * avec la règle appliquée (origine du taux, base, taux, montant). Une seule lecture serveur, `GET /sales/{id}/detail` :
 * le serveur ne rend à une vendeuse que ses ventes et sa commission. La facture et ses actions restent dans
 * `vvs-invoice-panel`.
 */
@Component({
  selector: 'vvs-sale-detail-panel',
  imports: [TranslatePipe, AriaryPipe, ListState, StatusBadge],
  templateUrl: './sale-detail-panel.html',
  styleUrl: './sale-detail-panel.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SaleDetailPanel {
  private readonly salesService = inject(SalesService);

  /** Identifiant de la vente (pas sa référence) : c'est la clé de l'API. */
  readonly saleId = input.required<string>();

  protected readonly saleTones = SALE_TONES;
  protected readonly commissionTones = COMMISSION_TONES;
  protected readonly detail = rxResource({
    params: () => this.saleId(),
    stream: ({ params }) => this.salesService.detail(params),
  });
  protected readonly soldAt = computed(() => {
    const sale = this.detail.value();
    return sale ? localDateTime(sale.createdAt) : '';
  });
}
