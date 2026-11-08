import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  linkedSignal,
  signal,
} from '@angular/core';
import { rxResource, takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { Plus, TriangleAlert, X } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthStore } from '../../../../core/auth/auth-store';
import { errorMessage } from '../../../../core/http/problem-detail';
import { LanguageService } from '../../../../core/i18n/language-service';
import { Icon } from '../../../../shared/components/icon/icon';
import { KpiCard } from '../../../../shared/components/kpi-card/kpi-card';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ShortDatePipe } from '../../../../shared/pipes/short-date-pipe';
import { LOT_STATUS_TONES } from '../../data-access/lot-tones';
import { toLotStatus } from '../../data-access/production-mapping';
import {
  DraftLine,
  costsOf,
  maxProducible,
  recipeLines,
  toRows,
} from '../../data-access/lot-draft';
import {
  LotRDto,
  LotStatus,
  ProducibleArticleRDto,
  ProductionBatchRDto,
} from '../../data-access/production-models';
import { CreatedLot, ProductionService } from '../../data-access/production-service';

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
  private readonly destroyRef = inject(DestroyRef);
  private readonly authStore = inject(AuthStore);

  protected readonly icons = { Plus, TriangleAlert, X };
  protected readonly lotTones = LOT_STATUS_TONES;
  protected readonly context = rxResource({ stream: () => this.productionService.context() });
  protected readonly submitting = signal(false);
  protected readonly created = signal<readonly LotRDto[]>([]);
  /** Statuts changés depuis l'écran (PRD-03), par identifiant de lot, sans recharger la liste. */
  private readonly statuses = signal<Readonly<Record<string, LotStatus>>>({});
  /** Lot dont le changement de statut est en cours d'envoi : ses boutons sont désactivés. */
  protected readonly changing = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    articleId: ['', Validators.required],
    quantity: [30, [Validators.required, Validators.min(1)]],
    operatorId: ['', Validators.required],
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
  protected readonly recentLots = computed(() => {
    const statuses = this.statuses();
    return [...this.created(), ...(this.context.value()?.recentLots ?? [])].map((lot) => {
      const status = statuses[lot.id];
      return status ? { ...lot, status } : lot;
    });
  });
  protected readonly numberFormat = computed(
    () => new Intl.NumberFormat(this.language.current(), { maximumFractionDigits: 3 }),
  );
  /** Valider mouvemente le stock (PRD-02) : réservé à `production.complete` ; le serveur décide. */
  protected readonly canComplete = computed(() =>
    this.authStore.hasPermission('production.complete'),
  );
  /** Démarrer un lot planifié (PRD-03) : `production.update` ; le serveur décide. */
  protected readonly canStart = computed(() => this.authStore.hasPermission('production.update'));
  protected readonly canValidate = computed(
    () =>
      this.form.valid &&
      this.rows().length > 0 &&
      this.shortRows().length === 0 &&
      this.canComplete() &&
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
    const { articleId, quantity, operatorId } = this.form.getRawValue();
    const lines = this.lines().map(({ materialId, quantity: used }) => ({
      materialId,
      quantity: used,
    }));
    const operator =
      this.context.value()?.operators.find((item) => item.id === operatorId)?.name ?? '';
    this.submitting.set(true);
    this.productionService
      .createLot(
        { articleId, quantity, operatorId, lines, validate },
        { articleName: this.product()?.name ?? '', operator },
      )
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.submitting.set(false)),
      )
      .subscribe({
        next: (result) => this.onCreated(result),
        error: (error: unknown) =>
          this.toast.show('production.saveError', { message: errorMessage(error) }, 'danger'),
      });
  }

  /** Fait avancer le statut d'un lot (PRD-03) : planifié → en cours, ou → terminé (stock mouvementé, PRD-02). */
  protected advance(lot: LotRDto, step: 'start' | 'finish'): void {
    const request =
      step === 'start'
        ? this.productionService.startLot(lot.id)
        : this.productionService.completeLot(lot.id);
    this.changing.set(lot.id);
    request
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.changing.set(null)),
      )
      .subscribe({
        next: ({ body }) => this.onAdvanced(lot, body),
        error: (error: unknown) =>
          this.toast.show(
            'production.statusError',
            { number: lot.number, message: errorMessage(error) },
            'danger',
          ),
      });
  }

  /** Sans réponse, la saisie attend le réseau : le statut affiché ne change qu'avec la réponse du serveur. */
  private onAdvanced(lot: LotRDto, batch: ProductionBatchRDto | undefined): void {
    if (!batch) {
      this.toast.show('common.queuedSaved', {}, 'info');
      return;
    }
    const status = toLotStatus(batch.status);
    this.statuses.update((all) => ({ ...all, [lot.id]: status }));
    this.toast.show(
      status === 'VALIDATED' ? 'production.validatedToast' : 'production.startedToast',
      { number: lot.number },
      'success',
    );
  }

  /** Lot validé (PRD-02) : stock mis à jour par le serveur ; sinon lot planifié (PRD-01). */
  private onCreated({ queued, lot }: CreatedLot): void {
    if (!lot) {
      this.toast.show('common.queuedSaved', {}, 'info');
      return;
    }
    this.created.update((all) => [lot, ...all]);
    this.toast.show(
      lot.status === 'VALIDATED' ? 'production.validatedToast' : 'production.draftToast',
      { number: lot.number },
      queued ? 'info' : 'success',
    );
  }
}
