import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CircleUser, History, Route } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../../../shared/components/icon/icon';
import { UnderConstruction } from '../../../shared/components/under-construction/under-construction';

/**
 * Coquille mobile du livreur (LIV-02) : contenu plein écran et barre d'onglets en bas,
 * à portée de pouce. Liens relatifs : la même coquille sert l'aperçu de développement.
 */
@Component({
  selector: 'vvs-driver-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslatePipe, Icon, UnderConstruction],
  templateUrl: './driver-layout.html',
  styleUrl: './driver-layout.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DriverLayout {
  protected readonly tabs = [
    { path: '.', labelKey: 'deliveries.driver.tab.round', icon: Route, exact: true },
    { path: 'history', labelKey: 'deliveries.driver.tab.history', icon: History, exact: false },
    { path: 'profile', labelKey: 'deliveries.driver.tab.profile', icon: CircleUser, exact: false },
  ] as const;
}
