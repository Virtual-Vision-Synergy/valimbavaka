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
import { Plus } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../../../../shared/components/icon/icon';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import {
  SalonProductRDto,
  SalonServiceWDto,
  TreatmentRDto,
} from '../../data-access/service-models';

interface ConsumedLine {
  readonly productId: string;
  readonly quantity: number;
}

/** Enregistrement d'une prestation (SRV-01, SRV-02) : soin, cliente, produits consommés pré-remplis. */
@Component({
  selector: 'vvs-salon-form',
  imports: [ReactiveFormsModule, TranslatePipe, AriaryPipe, Icon],
  templateUrl: './salon-form.html',
  styleUrl: './salon-form.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SalonForm {
  readonly treatments = input.required<readonly TreatmentRDto[]>();
  readonly products = input.required<readonly SalonProductRDto[]>();
  readonly staff = input.required<readonly string[]>();
  readonly registered = output<SalonServiceWDto>();

  protected readonly icons = { Plus };
  protected readonly treatmentId = signal('face');
  protected readonly attempted = signal(false);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    customer: [''],
    staff: ['', Validators.required],
  });

  protected readonly treatment = computed(() =>
    this.treatments().find((item) => item.id === this.treatmentId()),
  );
  /** Produits proposés par la recette du soin ; modifiables, remis à zéro quand le soin change. */
  protected readonly lines = linkedSignal<TreatmentRDto | undefined, readonly ConsumedLine[]>({
    source: this.treatment,
    computation: (treatment) =>
      treatment?.recipe.map(({ productId, quantity }) => ({ productId, quantity })) ?? [],
  });
  protected readonly rows = computed(() =>
    this.lines().flatMap((line) => {
      const product = this.products().find((item) => item.id === line.productId);
      return product ? [{ line, product, short: line.quantity > product.stock }] : [];
    }),
  );
  protected readonly addable = computed(() =>
    this.products().filter(
      (product) => !this.lines().some((line) => line.productId === product.id),
    ),
  );
  protected readonly hasShortage = computed(() => this.rows().some((row) => row.short));

  protected setQuantity(productId: string, raw: string): void {
    const quantity = Math.max(0, Number(raw.replace(',', '.')) || 0);
    this.lines.update((lines) =>
      lines.map((line) => (line.productId === productId ? { ...line, quantity } : line)),
    );
  }

  protected addProduct(productId: string): void {
    if (productId) {
      this.lines.update((lines) => [...lines, { productId, quantity: 1 }]);
    }
  }

  protected submit(): void {
    this.attempted.set(true);
    if (this.form.invalid || this.hasShortage()) {
      return;
    }
    const { customer, staff } = this.form.getRawValue();
    this.registered.emit({
      treatmentId: this.treatmentId(),
      customer: customer.trim() || null,
      staff,
      consumed: this.lines(),
    });
    this.form.reset();
    this.attempted.set(false);
  }

  protected cancel(): void {
    this.form.reset();
    this.attempted.set(false);
    this.treatmentId.set('face');
  }
}
