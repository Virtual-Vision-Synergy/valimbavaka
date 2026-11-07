import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  linkedSignal,
  output,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Plus, X } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../../../../shared/components/icon/icon';
import { StockRDto } from '../../../stocks/data-access/stock-models';
import { ShootingItemWDto, ShootingWDto } from '../../data-access/service-models';
import { toLocalTimestamp } from '../../data-access/shooting-mapping';

const DAY_MS = 24 * 60 * 60 * 1000;

/** Emplacement proposé par défaut : la boutique, dont le stock est vendu au comptoir. */
const DEFAULT_LOCATION = 'STORE';

/**
 * Sortie shooting (SHO-01) : emplacement, articles et quantités (pas plus que le disponible), retour prévu.
 * Le serveur reste juge (stock, dates) ; le formulaire évite seulement les envois voués à l'échec.
 */
@Component({
  selector: 'vvs-shooting-form',
  imports: [ReactiveFormsModule, TranslatePipe, Icon],
  templateUrl: './shooting-form.html',
  styleUrl: './shooting-form.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShootingForm {
  readonly stock = input.required<readonly StockRDto[]>();
  readonly saving = input(false);
  readonly submitted = output<ShootingWDto>();

  protected readonly icons = { Plus, X };
  protected readonly attempted = signal(false);
  protected readonly lines = signal<readonly ShootingItemWDto[]>([]);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    expectedReturn: [toLocalTimestamp(new Date(Date.now() + DAY_MS)), Validators.required],
    note: ['', Validators.maxLength(255)],
  });

  /** Emplacements actifs du référentiel, tels que le stock les détaille. */
  protected readonly locations = computed(() => this.stock()[0]?.locations ?? []);
  protected readonly locationId = linkedSignal(
    () =>
      this.locations().find((location) => location.code === DEFAULT_LOCATION)?.locationId ??
      this.locations()[0]?.locationId ??
      '',
  );
  protected readonly rows = computed(() =>
    this.lines().flatMap((line) => {
      const article = this.stock().find((item) => item.articleId === line.articleId);
      if (!article) {
        return [];
      }
      const available = this.available(article);
      return [{ line, article, available, short: line.quantity > available }];
    }),
  );
  protected readonly addable = computed(() =>
    this.stock().filter(
      (article) => !this.lines().some((line) => line.articleId === article.articleId),
    ),
  );
  protected readonly hasShortage = computed(() => this.rows().some((row) => row.short));
  protected readonly hasInvalidQuantity = computed(() =>
    this.lines().some((line) => !(line.quantity > 0)),
  );

  protected available(article: StockRDto): number {
    return (
      article.locations.find((location) => location.locationId === this.locationId())?.quantity ?? 0
    );
  }

  protected addArticle(articleId: string): void {
    if (articleId) {
      this.lines.update((lines) => [...lines, { articleId, quantity: 1 }]);
    }
  }

  protected removeArticle(articleId: string): void {
    this.lines.update((lines) => lines.filter((line) => line.articleId !== articleId));
  }

  protected setQuantity(articleId: string, raw: string): void {
    const quantity = Math.max(0, Number(raw.replace(',', '.')) || 0);
    this.lines.update((lines) =>
      lines.map((line) => (line.articleId === articleId ? { ...line, quantity } : line)),
    );
  }

  protected returnInPast(): boolean {
    const value = this.form.controls.expectedReturn.value;
    return !value || new Date(value).getTime() <= Date.now();
  }

  protected submit(): void {
    this.attempted.set(true);
    const invalid = this.form.invalid || this.returnInPast() || !this.locationId();
    if (invalid || !this.lines().length || this.hasShortage() || this.hasInvalidQuantity()) {
      return;
    }
    const { expectedReturn, note } = this.form.getRawValue();
    this.submitted.emit({
      locationId: this.locationId(),
      items: this.lines(),
      expectedReturnAt: new Date(expectedReturn).toISOString(),
      note: note.trim() || null,
    });
  }

  /** Remet le formulaire à zéro après un enregistrement réussi (appelé par la page). */
  reset(): void {
    this.lines.set([]);
    this.form.reset();
    this.attempted.set(false);
  }
}
