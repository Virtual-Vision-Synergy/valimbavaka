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
import { AuthStore } from '../../../../core/auth/auth-store';
import { errorMessage } from '../../../../core/http/problem-detail';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { StockLocationRDto } from '../../../referential/data-access/referential-models';
import { ReferentialService } from '../../../referential/data-access/referential-service';
import { StockRDto } from '../../../stocks/data-access/stock-models';
import { StockService } from '../../../stocks/data-access/stock-service';
import { StockReturnWDto } from '../../data-access/transfer-models';
import { TransferService } from '../../data-access/transfer-service';

/** Mêmes bornes que le serveur (`StockReturnWDto`). */
export const MAX_RETURN_LINES = 100;
const MAX_NOTE_LENGTH = 500;
/** 9 chiffres et 3 décimales au maximum (`@Digits(integer = 9, fraction = 3)`), virgule ou point. */
const QUANTITY_PATTERN = /^\d{1,9}([.,]\d{1,3})?$/;

type ReturnLineForm = FormGroup<{
  articleId: FormControl<string>;
  quantity: FormControl<string>;
}>;

/** Article proposé pour un retour : stock disponible à l'emplacement d'origine. */
export interface ReturnableArticle {
  readonly articleId: string;
  readonly code: string;
  readonly name: string;
  readonly available: number;
}

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

/** Origine et destination différentes, comme le serveur l'exige. */
function distinctRoute(group: AbstractControl): ValidationErrors | null {
  const from = group.get('fromLocationId')?.value as string;
  const to = group.get('toLocationId')?.value as string;
  return from && from === to ? { sameLocation: true } : null;
}

/**
 * Retour de stock (TRF-03) : articles non écoulés renvoyés d'un emplacement vers un autre, par défaut de la
 * boutique vers l'entrepôt. Le serveur déplace le stock et donne au transfert le statut dédié `RETURN`.
 */
@Component({
  selector: 'vvs-stock-return-page',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe],
  templateUrl: './stock-return-page.html',
  styleUrl: './stock-return-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StockReturnPage {
  private readonly transferService = inject(TransferService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder).nonNullable;

  /** La vendeuse n'a pas l'historique des transferts : elle revient aux stocks. */
  protected readonly backLink = inject(AuthStore).hasPermission('transfers.read')
    ? '/transfers'
    : '/stocks';
  protected readonly maxLines = MAX_RETURN_LINES;
  protected readonly locations = signal<readonly StockLocationRDto[]>([]);
  private readonly stocks = signal<readonly StockRDto[]>([]);
  protected readonly isSaving = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.fb.group(
    {
      fromLocationId: ['', Validators.required],
      toLocationId: ['', Validators.required],
      lines: this.fb.array([this.newLine()]),
      note: ['', Validators.maxLength(MAX_NOTE_LENGTH)],
    },
    { validators: distinctRoute },
  );

  private readonly fromLocationId = toSignal(this.form.controls.fromLocationId.valueChanges, {
    initialValue: '',
  });

  /** Articles ayant du stock à l'origine choisie, avec la quantité disponible. */
  protected readonly articles = computed<readonly ReturnableArticle[]>(() => {
    const from = this.fromLocationId();
    return this.stocks()
      .map((stock) => ({
        articleId: stock.articleId,
        code: stock.code,
        name: stock.name,
        available: stock.locations.find((item) => item.locationId === from)?.quantity ?? 0,
      }))
      .filter((article) => article.available > 0);
  });

  constructor() {
    inject(ReferentialService)
      .listStockLocations()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (locations) => this.useLocations(locations),
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
    inject(StockService)
      .quantities()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (stocks) => this.stocks.set(stocks),
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  protected get lines(): FormArray<ReturnLineForm> {
    return this.form.controls.lines;
  }

  /** Stock disponible à l'origine pour l'article de la ligne, ou null si aucun article n'est choisi. */
  protected available(index: number): number | null {
    const articleId = this.lines.at(index).controls.articleId.value;
    return this.articles().find((article) => article.articleId === articleId)?.available ?? null;
  }

  protected addLine(): void {
    if (this.lines.length < MAX_RETURN_LINES) {
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
    this.transferService
      .recordReturn(this.body())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSaving.set(false)),
      )
      .subscribe({
        next: (result) => {
          if (result.queued) {
            this.toast.show('common.queuedSaved', {}, 'info');
          } else if (result.body) {
            this.toast.show('transfers.return.saved', { number: result.body.number });
          }
          void this.router.navigate([this.backLink]);
        },
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  private body(): StockReturnWDto {
    const value = this.form.getRawValue();
    return {
      fromLocationId: value.fromLocationId,
      toLocationId: value.toLocationId,
      lines: value.lines.map((line) => ({
        articleId: line.articleId,
        quantity: toQuantity(line.quantity),
      })),
      note: value.note.trim() || null,
    };
  }

  /** Par défaut : de la boutique vers l'entrepôt (cas courant des invendus). */
  private useLocations(locations: readonly StockLocationRDto[]): void {
    this.locations.set(locations);
    const idOf = (code: string) => locations.find((location) => location.code === code)?.id ?? '';
    this.form.patchValue({
      fromLocationId: this.form.controls.fromLocationId.value || idOf('STORE'),
      toLocationId: this.form.controls.toLocationId.value || idOf('WAREHOUSE'),
    });
  }

  private newLine(): ReturnLineForm {
    return this.fb.group({
      articleId: ['', Validators.required],
      quantity: ['', [Validators.required, quantityValidator]],
    });
  }
}
