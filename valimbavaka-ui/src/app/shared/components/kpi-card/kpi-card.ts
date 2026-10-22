import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Tone } from '../../models/tone';
import { StatusBadge } from '../status-badge/status-badge';

export interface KpiTrend {
  readonly tone: Tone;
  readonly label: string;
}

/** Indicateur clé : libellé, valeur dominante, unité, tendance ; le détail (jauge, légende) est projeté. */
@Component({
  selector: 'vvs-kpi-card',
  imports: [StatusBadge],
  templateUrl: './kpi-card.html',
  styleUrl: './kpi-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KpiCard {
  readonly label = input.required<string>();
  readonly value = input.required<string>();
  readonly unit = input<string>();
  readonly trend = input<KpiTrend>();
}
