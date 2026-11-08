import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, finalize, map, of } from 'rxjs';
import { AuthStore } from '../../../../core/auth/auth-store';
import { errorMessage } from '../../../../core/http/problem-detail';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { ReferentialService } from '../../../referential/data-access/referential-service';
import { UserService } from '../../../user/data-access/user-service';
import { InventoryScope, InventorySessionWDto } from '../../data-access/inventory-models';
import { InventoryService } from '../../data-access/inventory-service';

/** Date du jour `AAAA-MM-JJ` dans le fuseau du navigateur. */
export function todayIso(now: Date = new Date()): string {
  const pad = (value: number): string => String(value).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

/** Gamme exigée pour le périmètre `RANGE` (même règle que le serveur, qui refuse aussi une gamme avec `ALL`). */
function rangeRequiredForRangeScope(group: AbstractControl): ValidationErrors | null {
  const scope = group.get('scope')?.value as InventoryScope;
  return scope === 'RANGE' && !group.get('rangeId')?.value ? { rangeRequired: true } : null;
}

/**
 * Ouverture d'une session d'inventaire (INV-01) : emplacement, périmètre, responsable et date de comptage sont
 * obligatoires. Le serveur crée les lignes à compter et refuse (409) une 2e session en cours sur l'emplacement.
 */
@Component({
  selector: 'vvs-inventory-open-page',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe],
  templateUrl: './inventory-open-page.html',
  styleUrl: './inventory-open-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InventoryOpenPage {
  private readonly inventoryService = inject(InventoryService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly scopes: readonly InventoryScope[] = ['ALL', 'RANGE'];
  protected readonly today = todayIso();
  protected readonly isSaving = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly locations = toSignal(
    inject(ReferentialService)
      .listStockLocations()
      .pipe(catchError(() => of([]))),
    { initialValue: [] },
  );
  protected readonly ranges = toSignal(
    inject(ReferentialService)
      .listProductRanges()
      .pipe(catchError(() => of([]))),
    { initialValue: [] },
  );
  /** Comptes actifs, triés par nom : tout compte actif peut être responsable du comptage. */
  protected readonly users = toSignal(
    inject(UserService)
      .list({ page: 1, size: 100, sortBy: 'lastName', sortDirection: 'ASC' })
      .pipe(
        map((page) => page.items.filter((user) => user.active)),
        catchError(() => of([])),
      ),
    { initialValue: [] },
  );

  protected readonly form = inject(FormBuilder).nonNullable.group(
    {
      locationId: ['', Validators.required],
      scope: ['ALL' as InventoryScope, Validators.required],
      rangeId: [''],
      // L'administrateur connecté est proposé par défaut ; il peut désigner un autre compte actif.
      responsibleId: [inject(AuthStore).currentUser()?.id ?? '', Validators.required],
      countDate: [this.today, Validators.required],
    },
    { validators: rangeRequiredForRangeScope },
  );

  constructor() {
    this.form.controls.scope.valueChanges.pipe(takeUntilDestroyed()).subscribe((scope) => {
      if (scope === 'ALL') {
        this.form.controls.rangeId.setValue('');
      }
    });
  }

  /** Erreur d'un champ touché, ou null ; la date passée est refusée comme par le serveur. */
  protected fieldError(field: keyof typeof this.form.controls): string | null {
    const control = this.form.controls[field];
    if (field === 'rangeId') {
      return this.form.hasError('rangeRequired') && control.touched
        ? 'inventory.open.rangeRequired'
        : null;
    }
    if (!control.touched) {
      return null;
    }
    if (control.invalid) {
      return 'inventory.open.required';
    }
    return field === 'countDate' && control.value < this.today ? 'inventory.open.pastDate' : null;
  }

  protected save(): void {
    if (this.isSaving()) {
      return;
    }
    this.form.markAllAsTouched();
    if (this.form.invalid || this.form.controls.countDate.value < this.today) {
      return;
    }
    this.isSaving.set(true);
    this.errorMessage.set(null);
    this.inventoryService
      .open(this.toRequest())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSaving.set(false)),
      )
      .subscribe({
        next: (result) => {
          if (result.queued) {
            this.toast.show('common.queuedSaved', {}, 'info');
          } else if (result.body) {
            this.toast.show('inventory.open.opened', { number: result.body.number });
          }
          void this.router.navigate(['/inventory']);
        },
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  private toRequest(): InventorySessionWDto {
    const value = this.form.getRawValue();
    return {
      locationId: value.locationId,
      scope: value.scope,
      rangeId: value.scope === 'RANGE' ? value.rangeId : null,
      responsibleId: value.responsibleId,
      countDate: value.countDate,
    };
  }
}
