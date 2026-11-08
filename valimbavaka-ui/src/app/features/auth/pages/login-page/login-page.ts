import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ArrowRight, Eye, EyeOff, Moon, Sun, WifiOff } from 'lucide';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth-service';
import { ApiError, errorMessage } from '../../../../core/http/problem-detail';
import { SESSION_EXPIRED_QUERY_PARAMS } from '../../../../core/http/session-interceptor';
import { LanguageSwitcher } from '../../../../core/i18n/language-switcher';
import { UiPreferencesService } from '../../../../core/layout/ui-preferences-service';
import { Icon } from '../../../../shared/components/icon/icon';

const HTTP_UNAUTHORIZED = 401;

/** Connexion : on n'envoie que l'email et le mot de passe, le rôle est celui du compte (AUTH-01). */
@Component({
  selector: 'vvs-login-page',
  imports: [ReactiveFormsModule, NgOptimizedImage, TranslatePipe, LanguageSwitcher, Icon],
  templateUrl: './login-page.html',
  styleUrl: './login-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);
  private readonly preferences = inject(UiPreferencesService);

  protected readonly icons = { ArrowRight, Eye, EyeOff, Moon, Sun, WifiOff };
  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly passwordVisible = signal(false);
  protected readonly capsLock = signal(false);
  protected readonly theme = this.preferences.theme;
  protected readonly roles = ['admin', 'seller', 'driver'] as const;
  /** Arrivée depuis une session expirée : les saisies en attente sont conservées jusqu'à la reconnexion (SYS-11). */
  protected readonly sessionExpired =
    inject(ActivatedRoute).snapshot.queryParamMap.get('session') ===
    SESSION_EXPIRED_QUERY_PARAMS.session;

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

  protected togglePassword(): void {
    this.passwordVisible.update((visible) => !visible);
  }

  protected toggleTheme(): void {
    this.preferences.toggleTheme();
  }

  /** Avertit quand les majuscules sont verrouillées pendant la saisie du mot de passe. */
  protected onPasswordKey(event: KeyboardEvent): void {
    this.capsLock.set(event.getModifierState('CapsLock'));
  }

  private messageFor(error: unknown): string {
    return error instanceof ApiError && error.status === HTTP_UNAUTHORIZED
      ? this.translate.instant('auth.login.invalidCredentials')
      : errorMessage(error);
  }
}
