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
import { AuthStore } from '../../../../core/auth/auth-store';
import { Icon } from '../../../../shared/components/icon/icon';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { SalonProductRDto, TreatmentRDto } from '../../data-access/service-models';
import {
  MAX_CUSTOMER_NAME_LENGTH,
  ServiceRecordWDto,
} from '../../data-access/service-record-models';

interface ConsumedLine {
  readonly productId: string;
  readonly quantity: number;
}

/**
 * Enregistrement d'une prestation (SRV-02) : soin actif du catalogue, cliente, produits consommés pré-remplis par la
 * recette et ajustables. La prestation est au nom du compte connecté ; le serveur déduit le stock salon.
 */
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
  readonly registered = output<ServiceRecordWDto>();

  protected readonly icons = { Plus };
  protected readonly maxCustomerLength = MAX_CUSTOMER_NAME_LENGTH;
  /** Premier soin de la liste par défaut ; revient au premier quand la liste change. */
  protected readonly treatmentId = linkedSignal(() => this.treatments()[0]?.id ?? '');
  protected readonly attempted = signal(false);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    customer: ['', Validators.maxLength(MAX_CUSTOMER_NAME_LENGTH)],
  });
  private readonly user = inject(AuthStore).currentUser;
  protected readonly sellerName = computed(() => {
    const user = this.user();
    return user ? `${user.firstName} ${user.lastName}` : '';
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
    const parsed = Math.max(0, Number(raw.replace(',', '.')) || 0);
    const quantity = Math.round(parsed * 1000) / 1000;
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
    if (this.form.invalid || this.hasShortage() || !this.treatment()) {
      return;
    }
    const { customer } = this.form.getRawValue();
    this.registered.emit({
      salonServiceId: this.treatmentId(),
      customerName: customer.trim() || null,
      products: this.lines()
        .filter((line) => line.quantity > 0)
        .map((line) => ({ articleId: line.productId, quantity: line.quantity })),
    });
    this.form.reset();
    this.attempted.set(false);
  }

  protected cancel(): void {
    this.form.reset();
    this.attempted.set(false);
    this.treatmentId.set(this.treatments()[0]?.id ?? '');
  }
}
