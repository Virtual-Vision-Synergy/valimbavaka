import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export interface ChartBar {
  readonly label: string;
  readonly value: number;
  /** Valeur affichée au-dessus de la barre et dans le tableau accessible. */
  readonly display: string;
  readonly highlight?: boolean;
}

export interface ChartTick {
  readonly value: number;
  readonly label: string;
}

/**
 * Histogramme léger en HTML/CSS (aucune librairie) : le graphique est décoratif pour les lecteurs
 * d'écran, qui lisent à la place un tableau de données équivalent (WCAG 1.1.1).
 */
@Component({
  selector: 'vvs-bar-chart',
  templateUrl: './bar-chart.html',
  styleUrl: './bar-chart.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BarChart {
  readonly caption = input.required<string>();
  readonly bars = input.required<readonly ChartBar[]>();
  /** Graduations de l'axe vertical, déjà formatées (ex. « 2 M ») ; la plus haute fixe l'échelle. */
  readonly ticks = input.required<readonly ChartTick[]>();

  private readonly max = computed(() => Math.max(...this.ticks().map((tick) => tick.value), 1));

  protected percent(value: number): number {
    return (value / this.max()) * 100;
  }
}
