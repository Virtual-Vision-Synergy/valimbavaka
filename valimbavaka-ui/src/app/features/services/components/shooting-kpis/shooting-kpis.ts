import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { KpiCard } from '../../../../shared/components/kpi-card/kpi-card';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ShootingSummaryRDto } from '../../data-access/service-models';

/**
 * Indicateurs en tête de l'onglet Shooting (SHO-03) : sorties en cours, retards (badge « À relancer » dès qu'il y en
 * a un) et valeur immobilisée au coût d'achat.
 */
@Component({
  selector: 'vvs-shooting-kpis',
  imports: [TranslatePipe, AriaryPipe, KpiCard],
  templateUrl: './shooting-kpis.html',
  styleUrl: './shooting-kpis.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShootingKpis {
  readonly summary = input.required<ShootingSummaryRDto>();
}
