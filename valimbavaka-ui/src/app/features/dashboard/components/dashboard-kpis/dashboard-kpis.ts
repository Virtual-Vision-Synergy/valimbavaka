import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ArrowRight } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { KpiCard, KpiTrend } from '../../../../shared/components/kpi-card/kpi-card';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { DashboardRDto, SalesChannel } from '../../data-access/dashboard-models';
import { Icon } from '../../../../shared/components/icon/icon';

const CHANNELS: readonly SalesChannel[] = ['SHOP', 'FACEBOOK', 'PHONE'];

/** Les 4 indicateurs du jour : chiffre d'affaires, ventes, livraisons, écart de stock (DSH-01). */
@Component({
  selector: 'vvs-dashboard-kpis',
  imports: [RouterLink, TranslatePipe, KpiCard, AriaryPipe, Icon],
  templateUrl: './dashboard-kpis.html',
  styleUrl: './dashboard-kpis.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardKpis {
  protected readonly icons = { ArrowRight };
  readonly data = input.required<DashboardRDto>();

  protected readonly revenueTrend = computed<KpiTrend>(() => {
    const change = this.data().revenue.changePercent;
    const label = `${change >= 0 ? '+' : ''}${change.toLocaleString('fr-FR')} %`;
    return { tone: change >= 0 ? 'success' : 'danger', label };
  });
  /** Écart signé avec un vrai signe moins typographique (U+2212). */
  protected readonly stockGapValue = computed(() => {
    const units = this.data().stockGap.units;
    return units > 0 ? `+${units}` : units < 0 ? `−${-units}` : '0';
  });
  protected readonly revenueProgress = computed(() =>
    Math.min(100, Math.round((this.data().revenue.amount / this.data().revenue.dailyTarget) * 100)),
  );
  protected readonly channelShares = computed(() => {
    const byChannel = this.data().sales.byChannel;
    const total = CHANNELS.reduce((sum, channel) => sum + byChannel[channel], 0) || 1;
    return CHANNELS.map((channel) => ({
      channel,
      count: byChannel[channel],
      share: (byChannel[channel] / total) * 100,
    }));
  });
  protected readonly deliveryShares = computed(() => {
    const { done, inProgress, failed, total } = this.data().deliveries;
    const percent = (value: number): number => (value / (total || 1)) * 100;
    return { done: percent(done), inProgress: percent(inProgress), failed: percent(failed) };
  });
}
