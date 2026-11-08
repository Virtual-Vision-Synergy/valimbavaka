import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { errorMessage } from '../../../../core/http/problem-detail';
import { SubmitResult } from '../../../../core/offline/outbox-models';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ArticleRDto } from '../../../article/data-access/article-models';
import { ArticleService } from '../../../article/data-access/article-service';
import { businessToday, driverLabel } from '../../data-access/delivery-mapping';
import {
  DeliveryDriverRDto,
  DeliveryOrderRDto,
  DeliveryOrderWDto,
} from '../../data-access/delivery-models';
import { DeliveryService } from '../../data-access/delivery-service';

/** Mêmes bornes que le serveur (`DeliveryOrderWDto`). */
export const MAX_ORDER_LINES = 100;
/** Chiffres, espaces, points ou tirets, un « + » initial et 7 chiffres au moins (`@Pattern` du serveur). */
const PHONE_PATTERN = /^\s*\+?[0-9][0-9 .-]{6,24}\s*$/;
/** 9 chiffres et 3 décimales au maximum (`@Digits(integer = 9, fraction = 3)`), virgule ou point. */
const QUANTITY_PATTERN = /^\d{1,9}([.,]\d{1,3})?$/;

type OrderLineForm = FormGroup<{
  articleId: FormControl<string>;
  quantity: FormControl<string>;
}>;

/** Quantité saisie (« 2,5 » ou « 2.5 ») convertie en nombre. */
export function toQuantity(value: string): number {
  return Number(value.trim().replace(',', '.'));
}

/** Quantité strictement positive, au format du serveur ; le champ vide relève de `required`. */
function quantityValidator(control: AbstractControl<string>): ValidationErrors | null {
  const value = control.value.trim();
  if (!value) {
    return null;
  }
  return QUANTITY_PATTERN.test(value) && toQuantity(value) > 0 ? null : { quantity: true };
}

/** Jour de livraison au plus tôt aujourd'hui (Antananarivo) ; le format ISO se compare comme une chaîne. */
function notPast(control: AbstractControl<string>): ValidationErrors | null {
  return control.value && control.value < businessToday() ? { past: true } : null;
}

/**
 * Création d'une commande à livrer (LIV-01) : client, adresse, livreur, jour de tournée et articles. Le serveur
 * lit les prix, attribue le numéro et ajoute la commande en fin de tournée du livreur (statut « À préparer »).
 */
@Component({
  selector: 'vvs-delivery-order-page',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe, AriaryPipe],
  templateUrl: './delivery-order-page.html',
  styleUrl: './delivery-order-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeliveryOrderPage {
  private readonly deliveryService = inject(DeliveryService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly maxLines = MAX_ORDER_LINES;
  protected readonly today = businessToday();
  protected readonly drivers = signal<readonly DeliveryDriverRDto[]>([]);
  protected readonly articles = signal<readonly ArticleRDto[]>([]);
  protected readonly isSaving = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly driverLabel = driverLabel;

  protected readonly form = this.fb.group({
    customerName: ['', [Validators.required, Validators.maxLength(120)]],
    customerPhone: ['', [Validators.required, Validators.pattern(PHONE_PATTERN)]],
    address: ['', [Validators.required, Validators.maxLength(255)]],
    district: ['', Validators.maxLength(80)],
    driverId: ['', Validators.required],
    scheduledDate: [this.today, [Validators.required, notPast]],
    lines: this.fb.array([this.newLine()]),
    note: ['', Validators.maxLength(500)],
  });

  private readonly lineValues = toSignal(this.form.controls.lines.valueChanges, {
    initialValue: this.form.controls.lines.getRawValue(),
  });

  /** Montant indicatif (prix actuels des articles) ; le serveur calcule le montant à encaisser. */
  protected readonly estimate = computed(() =>
    this.lineValues().reduce((sum, line) => {
      const price = this.articles().find((article) => article.id === line.articleId)?.price ?? 0;
      const quantity = toQuantity(line.quantity ?? '');
      return Number.isFinite(quantity) ? sum + price * quantity : sum;
    }, 0),
  );

  constructor() {
    this.deliveryService
      .drivers()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (drivers) => this.drivers.set(drivers),
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
    inject(ArticleService)
      .list({
        page: 1,
        size: 100,
        type: 'FINISHED',
        active: true,
        sortBy: 'name',
        sortDirection: 'ASC',
      })
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (page) => this.articles.set(page.items),
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  protected get lines(): FormArray<OrderLineForm> {
    return this.form.controls.lines;
  }

  protected addLine(): void {
    if (this.lines.length < MAX_ORDER_LINES) {
      this.lines.push(this.newLine());
    }
  }

  protected removeLine(index: number): void {
    if (this.lines.length > 1) {
      this.lines.removeAt(index);
    }
  }

  protected save(): void {
    if (this.isSaving()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSaving.set(true);
    this.errorMessage.set(null);
    this.deliveryService
      .create(this.body())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSaving.set(false)),
      )
      .subscribe({
        next: (result) => this.onSaved(result),
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  private onSaved(result: SubmitResult<DeliveryOrderRDto>): void {
    if (result.queued) {
      this.toast.show('common.queuedSaved', {}, 'info');
    } else if (result.body) {
      this.toast.show('deliveries.form.saved', {
        number: result.body.number,
        stop: result.body.stopNumber,
      });
    }
    void this.router.navigate(['/deliveries']);
  }

  private body(): DeliveryOrderWDto {
    const value = this.form.getRawValue();
    return {
      customerName: value.customerName.trim(),
      customerPhone: value.customerPhone.trim(),
      address: value.address.trim(),
      district: value.district.trim() || null,
      driverId: value.driverId,
      scheduledDate: value.scheduledDate,
      lines: value.lines.map((line) => ({
        articleId: line.articleId,
        quantity: toQuantity(line.quantity),
      })),
      note: value.note.trim() || null,
    };
  }

  private newLine(): OrderLineForm {
    return this.fb.group({
      articleId: ['', Validators.required],
      quantity: ['1', [Validators.required, quantityValidator]],
    });
  }
}
