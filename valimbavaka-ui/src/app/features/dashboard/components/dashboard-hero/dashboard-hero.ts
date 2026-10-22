import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Lock, PackagePlus, Store } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { HasPermissionDirective } from '../../../../core/auth/has-permission-directive';
import { LanguageService } from '../../../../core/i18n/language-service';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { DashboardRDto } from '../../data-access/dashboard-models';
import { Icon } from '../../../../shared/components/icon/icon';

/** Bandeau d'accueil : salutation selon l'heure, objectif mensuel, actions rapides autorisées par le rôle. */
@Component({
  selector: 'vvs-dashboard-hero',
  imports: [RouterLink, TranslatePipe, AriaryPipe, HasPermissionDirective, Icon],
  templateUrl: './dashboard-hero.html',
  styleUrl: './dashboard-hero.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardHero {
  protected readonly icons = { Lock, PackagePlus, Store };
  private readonly language = inject(LanguageService);

  readonly data = input.required<DashboardRDto>();
  readonly firstName = input.required<string>();
  /** Heure courante (0-23), injectable pour les tests. */
  readonly hour = input(new Date().getHours());

  protected readonly greetingKey = computed(() => {
    const hour = this.hour();
    // Avant 5 h on est encore « le soir » pour une équipe qui clôture tard.
    if (hour >= 5 && hour < 12) {
      return 'dashboard.hero.morning';
    }
    return hour >= 5 && hour < 18 ? 'dashboard.hero.afternoon' : 'dashboard.hero.evening';
  });
  protected readonly longDate = computed(() =>
    new Intl.DateTimeFormat(this.language.current(), {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    }).format(new Date(`${this.data().businessDate}T12:00:00`)),
  );
  protected readonly progress = computed(() =>
    Math.min(100, Math.round((this.data().monthlyRevenue / this.data().monthlyTarget) * 100)),
  );
}
