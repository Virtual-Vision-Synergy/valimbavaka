import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth-service';
import { ApiError, errorMessage } from '../../../../core/http/problem-detail';
import { LanguageSwitcher } from '../../../../core/i18n/language-switcher';

const HTTP_UNAUTHORIZED = 401;

/** Connexion : on n'envoie que l'email et le mot de passe, le rôle est celui du compte (AUTH-01). */
@Component({
  selector: 'vvs-login-page',
  imports: [ReactiveFormsModule, TranslatePipe, LanguageSwitcher],
  templateUrl: './login-page.html',
  styleUrl: './login-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  protected submit(): void {
    if (this.form.invalid || this.isSubmitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.authService
      .login(this.form.getRawValue())
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: () => void this.router.navigateByUrl('/'),
        error: (error: unknown) => this.errorMessage.set(this.messageFor(error)),
      });
  }

  protected hasError(control: 'email' | 'password'): boolean {
    const field = this.form.controls[control];
    return field.invalid && field.touched;
  }

  private messageFor(error: unknown): string {
    return error instanceof ApiError && error.status === HTTP_UNAUTHORIZED
      ? this.translate.instant('auth.login.invalidCredentials')
      : errorMessage(error);
  }
}
