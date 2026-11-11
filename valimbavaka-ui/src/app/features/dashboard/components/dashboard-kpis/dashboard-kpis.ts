import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ArrowRight } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { KpiCard, KpiTrend } from '../../../../shared/components/kpi-card/kpi-card';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { DashboardRDto } from '../../data-access/dashboard-models';
import { Icon } from '../../../../shared/components/icon/icon';

/** Nombre de teintes de canal (boutique, Facebook, téléphone) : au-delà, les canaux reprennent les teintes. */
const CHANNEL_SLOTS = 3;

/** Nombre avec un vrai signe (plus, ou moins typographique U+2212), au plus 2 décimales. */
function signed(value: number): string {
  const text = Math.abs(value).toLocaleString('fr-FR', { maximumFractionDigits: 2 });
  return value > 0 ? `+${text}` : value < 0 ? `−${text}` : '0';
}

/**
 * Les 4 indicateurs du jour : chiffre d'affaires, ventes, livraisons, écart de stock (DSH-01). Présentationnel : il
 * reçoit l'agrégat du serveur et ne recalcule rien d'autre que des proportions d'affichage.
 */
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

  /** Évolution par rapport à hier ; rien quand hier n'a pas de chiffre d'affaires à comparer. */
  protected readonly revenueTrend = computed<KpiTrend | undefined>(() => {
    const change = this.data().revenue.changePercent;
    if (change === null) {
      return undefined;
    }
    return { tone: change >= 0 ? 'success' : 'danger', label: `${signed(change)} %` };
  });
  /** Écart du nombre de ventes avec hier. */
  protected readonly salesTrend = computed<KpiTrend | undefined>(() => {
    const { count, previousCount } = this.data().sales;
    const delta = count - previousCount;
    return delta === 0
      ? undefined
      : { tone: delta > 0 ? 'success' : 'danger', label: signed(delta) };
  });
  protected readonly channelShares = computed(() => {
    const channels = this.data().sales.channels;
    const total = channels.reduce((sum, channel) => sum + channel.salesCount, 0) || 1;
    return channels.map((channel, index) => ({
      id: channel.salesChannelId,
      label: channel.salesChannelLabel,
      count: channel.salesCount,
      slot: index % CHANNEL_SLOTS,
      share: (channel.salesCount / total) * 100,
    }));
  });
  protected readonly deliveryShares = computed(() => {
    const { delivered, onRound, toPrepare, failed, total } = this.data().deliveries;
    const percent = (value: number): number => (value / (total || 1)) * 100;
    return {
      done: percent(delivered),
      inProgress: percent(onRound + toPrepare),
      failed: percent(failed),
    };
  });
  /** Écart signé ; `—` tant que la journée n'est pas ouverte (rien à réconcilier). */
  protected readonly stockGapValue = computed(() => {
    const gap = this.data().stockGap;
    return gap === null ? '—' : signed(gap.units);
  });
}
