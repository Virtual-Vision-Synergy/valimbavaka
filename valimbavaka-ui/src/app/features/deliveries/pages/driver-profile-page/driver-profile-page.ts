import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { LogOut, Moon, Sun } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../../../core/auth/auth-service';
import { AuthStore } from '../../../../core/auth/auth-store';
import { LanguageSwitcher } from '../../../../core/i18n/language-switcher';
import { UiPreferencesService } from '../../../../core/layout/ui-preferences-service';
import { AvatarInitials } from '../../../../shared/components/avatar-initials/avatar-initials';
import { Icon } from '../../../../shared/components/icon/icon';

/** Profil du livreur : langue, thème (lisibilité en plein soleil) et déconnexion. */
@Component({
  selector: 'vvs-driver-profile-page',
  imports: [TranslatePipe, Icon, AvatarInitials, LanguageSwitcher],
  templateUrl: './driver-profile-page.html',
  styleUrl: './driver-profile-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DriverProfilePage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly preferences = inject(UiPreferencesService);
  protected readonly user = inject(AuthStore).currentUser;
  protected readonly icons = { LogOut, Moon, Sun };

  protected logout(): void {
    this.authService.logout().subscribe(() => void this.router.navigate(['/login']));
  }
}
