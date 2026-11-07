import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

/** Part du stock dans un emplacement : `labelKey` est une clé de traduction (ex. `stocks.location.SHOP`). */
export interface StockGaugeSegment {
  readonly key: string;
  readonly labelKey: string;
  readonly quantity: number;
}

interface GaugeBar extends StockGaugeSegment {
  /** Largeur dans la jauge, en % de la pleine échelle. */
  readonly width: number;
  /** Part du total, en % arrondi. */
  readonly share: number;
}

interface TipPosition {
  readonly top: number;
  readonly left: number;
}

let nextId = 0;

/**
 * Jauge de stock : rouge à zéro, ocre sous le seuil, teal au-dessus ; repère du seuil minimum en option.
 * Sans `segments`, elle est décorative (la valeur chiffrée est affichée à côté). Avec `segments` (STK-02),
 * elle montre la répartition par emplacement et son survol ou son focus affiche le détail chiffré.
 */
@Component({
  selector: 'vvs-stock-gauge',
  imports: [TranslatePipe],
  templateUrl: './stock-gauge.html',
  styleUrl: './stock-gauge.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[attr.aria-hidden]': 'detailed() ? null : "true"',
    '(window:scroll)': 'close()',
  },
})
export class StockGauge {
  readonly value = input.required<number>();
  /** Valeur pleine échelle (par défaut : le double du seuil). */
  readonly max = input<number>();
  readonly threshold = input.required<number>();
  readonly showThreshold = input(false);
  /** Répartition par emplacement ; vide = jauge simple. */
  readonly segments = input<readonly StockGaugeSegment[]>([]);

  protected readonly tipId = `vvs-stock-gauge-tip-${nextId++}`;
  protected readonly position = signal<TipPosition | null>(null);
  private readonly track = viewChild<ElementRef<HTMLElement>>('track');

  protected readonly detailed = computed(() => this.segments().length > 0);
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
  protected readonly total = computed(() =>
    this.segments().reduce((sum, segment) => sum + Math.max(0, segment.quantity), 0),
  );
  /** Au-delà de la pleine échelle, les segments sont réduits pour tenir dans la jauge. */
  protected readonly bars = computed<readonly GaugeBar[]>(() => {
    const total = this.total();
    const base = Math.max(total, this.scale());
    return this.segments().map((segment) => {
      const quantity = Math.max(0, segment.quantity);
      return {
        ...segment,
        width: (quantity / base) * 100,
        share: total === 0 ? 0 : Math.round((quantity / total) * 100),
      };
    });
  });

  protected open(): void {
    const rect = this.track()?.nativeElement.getBoundingClientRect();
    if (rect) {
      this.position.set({ top: rect.top, left: rect.left + rect.width / 2 });
    }
  }

  protected close(): void {
    if (this.position()) {
      this.position.set(null);
    }
  }
}
