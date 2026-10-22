import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * Jauge de stock décorative (la valeur chiffrée est toujours affichée à côté) : rouge à zéro,
 * ocre sous le seuil, teal au-dessus ; repère du seuil minimum en option.
 */
@Component({
  selector: 'vvs-stock-gauge',
  templateUrl: './stock-gauge.html',
  styleUrl: './stock-gauge.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { 'aria-hidden': 'true' },
})
export class StockGauge {
  readonly value = input.required<number>();
  /** Valeur pleine échelle (par défaut : le double du seuil). */
  readonly max = input<number>();
  readonly threshold = input.required<number>();
  readonly showThreshold = input(false);

  private readonly scale = computed(() => Math.max(1, this.max() ?? this.threshold() * 2));
  protected readonly fill = computed(() => Math.min(100, (this.value() / this.scale()) * 100));
  protected readonly marker = computed(() =>
    Math.min(100, (this.threshold() / this.scale()) * 100),
  );
  protected readonly level = computed(() => {
    const value = this.value();
    if (value === 0) {
      return 'out';
    }
    return value < this.threshold() ? 'low' : 'ok';
  });
}
