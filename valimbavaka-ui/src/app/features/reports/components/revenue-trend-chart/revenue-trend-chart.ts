import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language-service';
import { formatAriary } from '../../../../shared/pipes/ariary-pipe';
import { ReportGranularity, ReportTrendPointRDto } from '../../data-access/report-models';

/** Hauteur de repère « jolie » au-dessus du maximum (pas de 500 k, 1 M, 5 M…). */
function niceMax(value: number): number {
  const step = 10 ** Math.floor(Math.log10(Math.max(value, 1)));
  return Math.ceil(value / step) * step;
}

/** Une barre de la courbe : libellé court sous la barre, libellé complet pour le tableau, jour courant. */
interface ChartBar {
  readonly key: string;
  readonly short: string;
  readonly long: string;
  readonly revenue: number;
  readonly today: boolean;
}

/**
 * Courbe du chiffre d'affaires de la période (RPT-03) : une barre par jour (jour ou mois) ou par mois (année),
 * les jours sans vente valent 0. Décoratif pour les lecteurs d'écran, qui lisent le tableau de données
 * équivalent (WCAG 1.1.1) ; défilement horizontal sur mobile.
 */
@Component({
  selector: 'vvs-revenue-trend-chart',
  imports: [TranslatePipe],
  templateUrl: './revenue-trend-chart.html',
  styleUrl: './revenue-trend-chart.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RevenueTrendChart {
  private readonly language = inject(LanguageService);

  readonly caption = input.required<string>();
  readonly points = input.required<readonly ReportTrendPointRDto[]>();
  readonly granularity = input.required<ReportGranularity>();
  /** Aujourd'hui (`yyyy-MM-dd`) : met en valeur le jour (ou le mois) en cours. */
  readonly today = input.required<string>();
  readonly revenueLabel = input.required<string>();

  protected readonly bars = computed<readonly ChartBar[]>(() => {
    const yearly = this.granularity() === 'YEAR';
    const current = yearly ? this.today().slice(0, 7) : this.today();
    const locale = this.language.current();
    const short = new Intl.DateTimeFormat(locale, { month: 'short', timeZone: 'UTC' });
    const long = new Intl.DateTimeFormat(locale, {
      ...(yearly ? {} : { day: 'numeric' }),
      month: 'long',
      year: 'numeric',
      timeZone: 'UTC',
    });
    return this.points().map((point) => {
      const date = new Date(`${point.date}T00:00:00Z`);
      return {
        key: point.date,
        short: yearly ? short.format(date) : String(date.getUTCDate()),
        long: long.format(date),
        revenue: point.revenue,
        today: (yearly ? point.date.slice(0, 7) : point.date) === current,
      };
    });
  });
  protected readonly max = computed(() =>
    niceMax(Math.max(...this.points().map((p) => p.revenue), 1)),
  );
  protected readonly ticks = computed(() =>
    [0, 0.25, 0.5, 0.75, 1].map((ratio) => ({
      ratio: ratio * 100,
      label: ratio === 0 ? '0' : formatAriary(this.max() * ratio, 'compact'),
    })),
  );

  protected percent(value: number): number {
    return (value / this.max()) * 100;
  }

  protected format(value: number): string {
    return formatAriary(Math.round(value));
  }
}
