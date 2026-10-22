import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ChevronLeft, LogOut, UserPlus, X } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { filter } from 'rxjs';
import { AvatarInitials } from '../../../shared/components/avatar-initials/avatar-initials';
import { AuthService } from '../../auth/auth-service';
import { AuthStore } from '../../auth/auth-store';
import { HasPermissionDirective } from '../../auth/has-permission-directive';
import { LanguageSwitcher } from '../../i18n/language-switcher';
import { visibleSections } from '../menu';
import { TopBar } from '../top-bar/top-bar';
import { UiPreferencesService } from '../ui-preferences-service';
import { Icon } from '../../../shared/components/icon/icon';

/** Coquille applicative : le menu n'affiche que les modules autorisés par le rôle du compte (AUTH-01). */
@Component({
  selector: 'vvs-main-layout',
  imports: [
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    NgOptimizedImage,
    TranslatePipe,
    LanguageSwitcher,
    TopBar,
    AvatarInitials,
    HasPermissionDirective,
    Icon,
  ],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class.collapsed]': 'collapsed()',
    '[class.drawer-open]': 'drawerOpen()',
    '(click)': 'onHostClick($event)',
    '(keydown.escape)': 'closeDrawer()',
  },
})
export class MainLayout {
  protected readonly icons = { ChevronLeft, LogOut, UserPlus, X };
  private readonly authStore = inject(AuthStore);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly preferences = inject(UiPreferencesService);

  protected readonly user = this.authStore.currentUser;
  protected readonly sections = computed(() =>
    visibleSections((permission) => this.authStore.hasPermission(permission)),
  );
  protected readonly fullName = computed(() => {
    const user = this.user();
    return user ? `${user.firstName} ${user.lastName}` : '';
  });
  protected readonly collapsed = this.preferences.sidebarCollapsed;
  /** Tiroir de navigation (écrans < 1024 px). */
  protected readonly drawerOpen = signal(false);

  constructor() {
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.drawerOpen.set(false));
  }

  protected toggleSidebar(): void {
    this.preferences.toggleSidebar();
  }

  protected openDrawer(): void {
    this.drawerOpen.set(true);
  }

  protected closeDrawer(): void {
    this.drawerOpen.set(false);
  }

  /** Clic sur le voile du tiroir (décoratif, la fermeture clavier passe par Échap ou le bouton). */
  protected onHostClick(event: MouseEvent): void {
    if (event.target instanceof HTMLElement && event.target.classList.contains('scrim')) {
      this.closeDrawer();
    }
  }

  protected logout(): void {
    this.authService.logout().subscribe(() => void this.router.navigate(['/login']));
  }
}
