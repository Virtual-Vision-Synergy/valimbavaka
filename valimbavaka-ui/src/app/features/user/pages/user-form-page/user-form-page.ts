import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable, finalize, map } from 'rxjs';
import { ApiError, errorMessage } from '../../../../core/http/problem-detail';
import { SubmitResult } from '../../../../core/offline/outbox-models';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { RoleService } from '../../data-access/role-service';
import { SalesChannelService } from '../../data-access/sales-channel-service';
import { UserService } from '../../data-access/user-service';
import { CreateUserWDto, UserRDto, roleLabel } from '../../data-access/user-models';

/** 15 caractères minimum sans MFA (convention §9.2), comme le serveur. */
const MIN_PASSWORD_LENGTH = 15;
const MAX_NAME_LENGTH = 100;
const MIN_COMMISSION_RATE = 0;
const MAX_COMMISSION_RATE = 100;
const MIN_MONTHLY_TARGET = 0.01;

@Component({
  selector: 'vvs-user-form-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './user-form-page.html',
  styleUrl: './user-form-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserFormPage implements OnInit {
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  /** Renseigné par le paramètre de route `:id` (absent en création). */
  readonly id = input<string>();

  protected readonly roleLabel = roleLabel;
  protected readonly isEdit = computed(() => !!this.id());
  protected readonly roles = toSignal(inject(RoleService).listAll(), { initialValue: [] });
  protected readonly salesChannels = toSignal(inject(SalesChannelService).listAll(), {
    initialValue: [],
  });
  protected readonly isSaving = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly minPasswordLength = MIN_PASSWORD_LENGTH;

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(MIN_PASSWORD_LENGTH)]],
    firstName: ['', [Validators.required, Validators.maxLength(MAX_NAME_LENGTH)]],
    lastName: ['', [Validators.required, Validators.maxLength(MAX_NAME_LENGTH)]],
    roleId: ['', Validators.required],
    salesChannelId: ['', Validators.required],
    commissionRate: new FormControl<number | null>(null, [
      Validators.required,
      Validators.min(MIN_COMMISSION_RATE),
      Validators.max(MAX_COMMISSION_RATE),
    ]),
    monthlyTarget: new FormControl<number | null>(null, [Validators.min(MIN_MONTHLY_TARGET)]),
    active: [true],
  });

  private readonly selectedRoleId = toSignal(this.form.controls.roleId.valueChanges, {
    initialValue: this.form.controls.roleId.value,
  });

  /** Canal et commission n'existent que pour une vendeuse : le rôle choisi pilote le formulaire (AUTH-03). */
  protected readonly isSeller = computed(
    () => this.roles().find((role) => role.id === this.selectedRoleId())?.name === 'SELLER',
  );

  constructor() {
    effect(() => this.toggleSellerFields(this.isSeller()));
  }

  ngOnInit(): void {
    const id = this.id();
    if (!id) {
      return;
    }
    // Le mot de passe ne se modifie pas via ce formulaire
    this.form.controls.password.disable();
    this.userService
      .getById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (user) => this.fillForm(user),
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  protected hasError(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && control.touched;
  }

  /** Message renvoyé par le serveur (422) pour ce champ, s'il existe. */
  protected serverError(controlName: keyof typeof this.form.controls): string | null {
    const errors = this.form.controls[controlName].errors;
    return typeof errors?.['server'] === 'string' ? errors['server'] : null;
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSaving.set(true);
    this.errorMessage.set(null);
    this.saveRequest()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isSaving.set(false)),
      )
      .subscribe({
        next: (result) => {
          if (result.queued) {
            this.toast.show('common.queuedSaved', {}, 'info');
          }
          void this.router.navigate(['/users']);
        },
        error: (error: unknown) => this.showError(error),
      });
  }

  private saveRequest(): Observable<SubmitResult<UserRDto>> {
    const { email, password, firstName, lastName, roleId, active, ...terms } =
      this.form.getRawValue();
    const sellerTerms = this.sellerTerms(terms);
    const id = this.id();
    return id
      ? this.userService.update(id, { email, firstName, lastName, roleId, ...sellerTerms, active })
      : this.userService
          .create({ email, password, firstName, lastName, roleId, ...sellerTerms })
          .pipe(map((user): SubmitResult<UserRDto> => ({ queued: false, body: user })));
  }

  /** Canal, taux et objectif mensuel (COM-04) : envoyés pour une vendeuse seulement, `null` sinon. */
  private sellerTerms(terms: {
    salesChannelId: string;
    commissionRate: number | null;
    monthlyTarget: number | null;
  }): Pick<CreateUserWDto, 'salesChannelId' | 'commissionRate' | 'monthlyTarget'> {
    const isSeller = this.isSeller();
    return {
      salesChannelId: isSeller ? terms.salesChannelId || null : null,
      commissionRate: isSeller ? terms.commissionRate : null,
      monthlyTarget: isSeller ? terms.monthlyTarget || null : null,
    };
  }

  /** Les champs vendeuse ne sont validés ni envoyés pour un autre rôle. */
  private toggleSellerFields(isSeller: boolean): void {
    const { salesChannelId, commissionRate, monthlyTarget } = this.form.controls;
    for (const control of [salesChannelId, commissionRate, monthlyTarget]) {
      if (isSeller) {
        control.enable({ emitEvent: false });
      } else {
        control.disable({ emitEvent: false });
      }
    }
  }

  /** La source de vérité est le serveur : ses erreurs 422 sont réaffectées aux champs (§10.3). */
  private showError(error: unknown): void {
    this.errorMessage.set(errorMessage(error));
    if (!(error instanceof ApiError)) {
      return;
    }
    for (const { field, message } of error.fieldErrors) {
      const control = this.form.get(field);
      control?.setErrors({ server: message });
      control?.markAsTouched();
    }
  }

  private fillForm(user: UserRDto): void {
    this.form.patchValue({
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
      roleId: user.role.id,
      salesChannelId: user.salesChannelId ?? '',
      commissionRate: user.commissionRate,
      monthlyTarget: user.monthlyTarget,
      active: user.active,
    });
  }
}
