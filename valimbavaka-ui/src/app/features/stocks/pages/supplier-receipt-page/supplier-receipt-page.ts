import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize, map } from 'rxjs';
import { errorMessage } from '../../../../core/http/problem-detail';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { StockLocationRDto } from '../../../referential/data-access/referential-models';
import { ReferentialService } from '../../../referential/data-access/referential-service';
import { SupplierRDto } from '../../../supplier/data-access/supplier-models';
import { SupplierService } from '../../../supplier/data-access/supplier-service';
import { RestockOrderRDto, StockRDto, SupplierReceiptWDto } from '../../data-access/stock-models';
import { StockService } from '../../data-access/stock-service';

/** Mêmes bornes que le serveur (`SupplierReceiptWDto`). */
export const MAX_RECEIPT_LINES = 100;
const MAX_NOTE_LENGTH = 500;
const ACTIVE_SUPPLIERS = { page: 1, size: 100, sortBy: 'name' } as const;
/** `@Digits(integer = 9, fraction = 3)` pour la quantité, `@Digits(integer = 12, fraction = 2)` pour le coût. */
const QUANTITY_PATTERN = /^\d{1,9}([.,]\d{1,3})?$/;
const COST_PATTERN = /^\d{1,12}([.,]\d{1,2})?$/;

type ReceiptLineForm = FormGroup<{
  articleId: FormControl<string>;
  quantity: FormControl<string>;
  unitCost: FormControl<string>;
}>;

/** Nombre saisi (« 2,5 » ou « 2.5 ») converti en nombre. */
export function toNumber(value: string): number {
  return Number(value.trim().replace(',', '.'));
}

/** Nombre au format du serveur, strictement positif si demandé ; le champ vide relève de `required`. */
function numberValidator(pattern: RegExp, positive: boolean): ValidatorFn {
  return (control: AbstractControl<string>): ValidationErrors | null => {
    const value = control.value.trim();
    if (!value) {
      return null;
    }
    const valid = pattern.test(value) && (!positive || toNumber(value) > 0);
    return valid ? null : { number: true };
  };
}

/**
 * Réception d'une livraison fournisseur (STK-06) : l'administrateur saisit le fournisseur, l'emplacement
 * (l'entrepôt par défaut) et les articles reçus, librement ou en reprenant un bon de réapprovisionnement.
 * Le serveur fait entrer le stock et trace la réception ; une erreur se corrige par contre-passation.
 */
@Component({
  selector: 'vvs-supplier-receipt-page',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe],
  templateUrl: './supplier-receipt-page.html',
  styleUrl: './supplier-receipt-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SupplierReceiptPage {
  private readonly stockService = inject(StockService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly maxLines = MAX_RECEIPT_LINES;
  protected readonly suppliers = signal<readonly SupplierRDto[]>([]);
  protected readonly locations = signal<readonly StockLocationRDto[]>([]);
  protected readonly orders = signal<readonly RestockOrderRDto[]>([]);
  protected readonly articles = signal<readonly StockRDto[]>([]);
  protected readonly isSaving = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.fb.group({
    supplierId: ['', Validators.required],
    locationId: ['', Validators.required],
    restockOrderId: [''],
    lines: this.fb.array([this.newLine()]),
    note: ['', Validators.maxLength(MAX_NOTE_LENGTH)],
  });

  constructor() {
    const fail = (error: unknown) => this.errorMessage.set(errorMessage(error));
    inject(SupplierService)
      .list(ACTIVE_SUPPLIERS)
      .pipe(
        map((page) => page.items.filter((supplier) => supplier.active)),
        takeUntilDestroyed(),
      )
      .subscribe({ next: (suppliers) => this.suppliers.set(suppliers), error: fail });
    inject(ReferentialService)
      .listStockLocations()
      .pipe(takeUntilDestroyed())
      .subscribe({ next: (locations) => this.useLocations(locations), error: fail });
    this.stockService
      .pendingRestockOrders()
      .pipe(takeUntilDestroyed())
      .subscribe({ next: (orders) => this.orders.set(orders), error: fail });
    this.stockService
      .quantities()
      .pipe(takeUntilDestroyed())
      .subscribe({ next: (articles) => this.articles.set(articles), error: fail });
  }

  protected get lines(): FormArray<ReceiptLineForm> {
    return this.form.controls.lines;
  }

  protected addLine(): void {
    if (this.lines.length < MAX_RECEIPT_LINES) {
      this.lines.push(this.newLine());
    }
  }

  protected removeLine(index: number): void {
    if (this.lines.length > 1) {
      this.lines.removeAt(index);
    }
  }

  /**
   * Reprise d'un bon : une ligne par article du bon, quantité commandée et coût d'achat actuel de l'article ;
   * le fournisseur est prérempli s'il est le même pour tout le bon. Tout reste modifiable avant validation.
   */
  protected useOrder(): void {
    const order = this.orders().find((item) => item.id === this.form.controls.restockOrderId.value);
    if (!order) {
      return;
    }
    this.lines.clear();
    for (const line of order.lines) {
      const cost = this.articles().find((article) => article.articleId === line.articleId)?.cost;
      this.lines.push(
        this.newLine(line.articleId, String(line.quantity), cost == null ? '' : String(cost)),
      );
    }
    const supplierIds = new Set(order.lines.map((line) => line.supplierId));
    const [supplierId] = [...supplierIds];
    if (supplierIds.size === 1 && supplierId) {
      this.form.controls.supplierId.setValue(supplierId);
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
    this.stockService
      .recordReceipt(this.body())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSaving.set(false)),
      )
      .subscribe({
        next: (result) => {
          if (result.queued) {
            this.toast.show('common.queuedSaved', {}, 'info');
          } else if (result.body) {
            this.toast.show('stocks.receipt.saved', { number: result.body.number });
          }
          void this.router.navigate(['/stocks']);
        },
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  private body(): SupplierReceiptWDto {
    const value = this.form.getRawValue();
    return {
      supplierId: value.supplierId,
      locationId: value.locationId,
      restockOrderId: value.restockOrderId || null,
      lines: value.lines.map((line) => ({
        articleId: line.articleId,
        quantity: toNumber(line.quantity),
        unitCost: toNumber(line.unitCost),
      })),
      note: value.note.trim() || null,
    };
  }

  /** Par défaut : l'entrepôt, comme le serveur quand aucun emplacement n'est donné. */
  private useLocations(locations: readonly StockLocationRDto[]): void {
    this.locations.set(locations);
    const warehouse = locations.find((location) => location.code === 'WAREHOUSE')?.id ?? '';
    if (!this.form.controls.locationId.value) {
      this.form.controls.locationId.setValue(warehouse);
    }
  }

  private newLine(articleId = '', quantity = '', unitCost = ''): ReceiptLineForm {
    return this.fb.group({
      articleId: [articleId, Validators.required],
      quantity: [quantity, [Validators.required, numberValidator(QUANTITY_PATTERN, true)]],
      unitCost: [unitCost, [Validators.required, numberValidator(COST_PATTERN, false)]],
    });
  }
}
