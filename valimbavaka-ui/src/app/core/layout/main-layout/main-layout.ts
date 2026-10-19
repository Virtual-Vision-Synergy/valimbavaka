import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../auth/auth-service';
import { AuthStore } from '../../auth/auth-store';
import { LanguageSwitcher } from '../../i18n/language-switcher';
import { visibleSections } from '../menu';

/** Coquille applicative : le menu n'affiche que les modules autorisés par le rôle du compte (AUTH-01). */
@Component({
  selector: 'vvs-main-layout',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, TranslatePipe, LanguageSwitcher],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayout {
  private readonly authStore = inject(AuthStore);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly user = this.authStore.currentUser;
  protected readonly sections = computed(() =>
    visibleSections((permission) => this.authStore.hasPermission(permission)),
  );
  protected readonly initial = computed(() => this.user()?.firstName.charAt(0).toUpperCase() ?? '');

  protected logout(): void {
    this.authService.logout().subscribe(() => void this.router.navigate(['/login']));
  }
}
