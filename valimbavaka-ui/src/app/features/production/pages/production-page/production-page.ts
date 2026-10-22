import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  linkedSignal,
  signal,
} from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Plus, TriangleAlert, X } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language-service';
import { Icon } from '../../../../shared/components/icon/icon';
import { KpiCard } from '../../../../shared/components/kpi-card/kpi-card';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { isoDay } from '../../../../shared/data-access/demo-dates';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ShortDatePipe } from '../../../../shared/pipes/short-date-pipe';
import { LOT_STATUS_TONES } from '../../data-access/lot-tones';
import {
  DraftLine,
  costsOf,
  maxProducible,
  recipeLines,
  toRows,
} from '../../data-access/lot-draft';
import { LotRDto, ProducibleArticleRDto } from '../../data-access/production-models';
import { ProductionService } from '../../data-access/production-service';

/** Production (PRD-01 à PRD-05) : un lot transforme des matières en produits finis, avec coût de revient. */
@Component({
  selector: 'vvs-production-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    TranslatePipe,
    AriaryPipe,
    ShortDatePipe,
    Icon,
    KpiCard,
    ListState,
    PageHeader,
    StatusBadge,
  ],
  templateUrl: './production-page.html',
  styleUrl: './production-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductionPage {
  private readonly productionService = inject(ProductionService);
  private readonly toast = inject(ToastService);
  private readonly language = inject(LanguageService);

  protected readonly icons = { Plus, TriangleAlert, X };
  protected readonly lotTones = LOT_STATUS_TONES;
  protected readonly context = rxResource({ stream: () => this.productionService.context() });
  protected readonly submitting = signal(false);
  protected readonly created = signal<readonly LotRDto[]>([]);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    articleId: ['', Validators.required],
    quantity: [30, [Validators.required, Validators.min(1)]],
    operator: ['', Validators.required],
  });
  private readonly values = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });

  protected readonly product = computed(() =>
    this.context.value()?.products.find((item) => item.articleId === this.values().articleId),
  );
  protected readonly quantity = computed(() => Math.max(0, Number(this.values().quantity) || 0));
  /** Lignes éditables : recalculées depuis la recette quand le produit ou la quantité change. */
  protected readonly lines = linkedSignal<
    { product: ProducibleArticleRDto | undefined; quantity: number },
    readonly DraftLine[]
  >({
    source: () => ({ product: this.product(), quantity: this.quantity() }),
    computation: ({ product, quantity }) => recipeLines(product, quantity),
  });
  protected readonly rows = computed(() =>
    toRows(this.lines(), this.context.value()?.materials ?? []),
  );
  protected readonly shortRows = computed(() => this.rows().filter((row) => row.short));
  protected readonly maxQuantity = computed(() => maxProducible(this.rows(), this.product()));
  protected readonly costs = computed(() =>
    costsOf(this.rows(), this.quantity(), this.product()?.price),
  );
  protected readonly addable = computed(() =>
    (this.context.value()?.materials ?? []).filter(
      (material) => !this.lines().some((line) => line.materialId === material.id),
    ),
  );
  protected readonly recentLots = computed(() => [
    ...this.created(),
    ...(this.context.value()?.recentLots ?? []),
  ]);
  protected readonly numberFormat = computed(
    () => new Intl.NumberFormat(this.language.current(), { maximumFractionDigits: 3 }),
  );
  protected readonly canValidate = computed(
    () =>
      this.form.valid &&
      this.rows().length > 0 &&
      this.shortRows().length === 0 &&
      !this.submitting(),
  );

  protected setQuantity(materialId: string, raw: string): void {
    const value = Math.max(0, Number(raw.replace(',', '.')) || 0);
    this.lines.update((lines) =>
      lines.map((line) => (line.materialId === materialId ? { ...line, quantity: value } : line)),
    );
  }

  protected removeLine(materialId: string): void {
    this.lines.update((lines) => lines.filter((line) => line.materialId !== materialId));
  }

  protected addLine(materialId: string): void {
    if (materialId) {
      this.lines.update((lines) => [...lines, { materialId, quantity: 0 }]);
    }
  }

  protected applyMaximum(): void {
    const max = this.maxQuantity();
    if (max !== null) {
      this.form.controls.quantity.setValue(max);
    }
  }

  protected save(validate: boolean): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { articleId, quantity, operator } = this.form.getRawValue();
    const lines = this.lines().map(({ materialId, quantity: used }) => ({
      materialId,
      quantity: used,
    }));
    this.submitting.set(true);
    this.productionService
      .createLot({ articleId, quantity, operator, lines, validate })
      .subscribe((number) => this.onCreated(number, validate));
  }

  private onCreated(number: string, validate: boolean): void {
    const { quantity, operator } = this.form.getRawValue();
    this.submitting.set(false);
    this.created.update((all) => [
      {
        number,
        date: isoDay(0),
        articleName: this.product()?.name ?? '',
        quantity,
        operator,
        status: validate ? 'VALIDATED' : 'DRAFT',
      },
      ...all,
    ]);
    this.toast.show(validate ? 'production.validatedToast' : 'production.draftToast', { number });
  }
}
