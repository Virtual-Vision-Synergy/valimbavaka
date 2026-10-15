import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable, finalize } from 'rxjs';
import { ApiError, errorMessage } from '../../../../core/http/problem-detail';
import { RoleService } from '../../data-access/role-service';
import { UserService } from '../../data-access/user-service';
import { UserRDto, roleLabel } from '../../data-access/user-models';

const MIN_PASSWORD_LENGTH = 8;
const MAX_NAME_LENGTH = 100;

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
  private readonly destroyRef = inject(DestroyRef);

  /** Renseigné par le paramètre de route `:id` (absent en création). */
  readonly id = input<string>();

  protected readonly roleLabel = roleLabel;
  protected readonly isEdit = computed(() => !!this.id());
  protected readonly roles = toSignal(inject(RoleService).listAll(), { initialValue: [] });
  protected readonly isSaving = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly minPasswordLength = MIN_PASSWORD_LENGTH;

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(MIN_PASSWORD_LENGTH)]],
    firstName: ['', [Validators.required, Validators.maxLength(MAX_NAME_LENGTH)]],
    lastName: ['', [Validators.required, Validators.maxLength(MAX_NAME_LENGTH)]],
    roleId: ['', Validators.required],
    active: [true],
  });

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
        next: () => this.router.navigate(['/users']),
        error: (error: unknown) => this.showError(error),
      });
  }

  private saveRequest(): Observable<UserRDto> {
    const { email, password, firstName, lastName, roleId, active } = this.form.getRawValue();
    const id = this.id();
    return id
      ? this.userService.update(id, { email, firstName, lastName, roleId, active })
      : this.userService.create({ email, password, firstName, lastName, roleId });
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
      active: user.active,
    });
  }
}
