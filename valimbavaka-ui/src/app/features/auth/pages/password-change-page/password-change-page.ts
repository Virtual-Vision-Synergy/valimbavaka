import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth-service';
import { AuthStore } from '../../../../core/auth/auth-store';
import { errorMessage } from '../../../../core/http/problem-detail';

/** 15 caractères minimum sans MFA (convention §9.2), comme le serveur. */
export const MIN_PASSWORD_LENGTH = 15;
const MAX_PASSWORD_LENGTH = 128;

const sameAsNewPassword = (group: AbstractControl): ValidationErrors | null =>
  group.get('newPassword')?.value === group.get('confirmation')?.value ? null : { mismatch: true };

/**
 * Changement obligatoire du mot de passe provisoire (AUTH-01, SYS-05). Tant qu'il n'est pas fait, le serveur
 * n'accorde aucune permission ; une fois changé, un refresh rend au jeton les droits du rôle.
 */
@Component({
  selector: 'vvs-password-change-page',
  imports: [ReactiveFormsModule, TranslatePipe],
  templateUrl: './password-change-page.html',
  styleUrl: './password-change-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PasswordChangePage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly user = inject(AuthStore).currentUser;
  protected readonly minLength = MIN_PASSWORD_LENGTH;
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group(
    {
      currentPassword: ['', Validators.required],
      newPassword: [
        '',
        [
          Validators.required,
          Validators.minLength(MIN_PASSWORD_LENGTH),
          Validators.maxLength(MAX_PASSWORD_LENGTH),
        ],
      ],
      confirmation: ['', Validators.required],
    },
    { validators: sameAsNewPassword },
  );

  protected submit(): void {
    if (this.form.invalid || this.isSubmitting()) {
      this.form.markAllAsTouched();
      return;
    }
    const { currentPassword, newPassword } = this.form.getRawValue();
    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.authService
      .changePassword({ currentPassword, newPassword })
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: () => void this.router.navigateByUrl('/'),
        error: (error: unknown) => this.errorMessage.set(errorMessage(error)),
      });
  }

  protected logout(): void {
    this.authService.logout().subscribe(() => void this.router.navigateByUrl('/login'));
  }

  protected hasError(control: 'currentPassword' | 'newPassword' | 'confirmation'): boolean {
    const field = this.form.controls[control];
    return field.invalid && field.touched;
  }

  protected isMismatch(): boolean {
    return this.form.hasError('mismatch') && this.form.controls.confirmation.touched;
  }
}
