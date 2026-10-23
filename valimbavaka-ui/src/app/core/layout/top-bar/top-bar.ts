import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  inject,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { Bell, ChevronRight, LogOut, Menu, Moon, Search, Sun } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { filter, map } from 'rxjs';
import { AuthService } from '../../auth/auth-service';
import { LanguageService } from '../../i18n/language-service';
import { SyncStatus } from '../../offline/sync-status';
import { findMenuEntry } from '../menu';
import { NotificationPanel } from '../notification-panel/notification-panel';
import { NotificationService } from '../notification-panel/notification-service';
import { SearchDialog } from '../search-dialog/search-dialog';
import { UiPreferencesService } from '../ui-preferences-service';
import { Icon } from '../../../shared/components/icon/icon';

/**
 * Barre supérieure : fil d'Ariane, recherche globale (Ctrl K), synchronisation, date, thème, notifications et
 * déconnexion, visible en permanence (AUTH-02).
 */
@Component({
  selector: 'vvs-top-bar',
  imports: [RouterLink, TranslatePipe, SyncStatus, SearchDialog, NotificationPanel, Icon],
  templateUrl: './top-bar.html',
  styleUrl: './top-bar.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown)': 'onShortcut($event)' },
})
export class TopBar {
  protected readonly icons = { Bell, ChevronRight, LogOut, Menu, Moon, Search, Sun };
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly preferences = inject(UiPreferencesService);
  private readonly language = inject(LanguageService);
  private readonly searchButton = viewChild.required<ElementRef<HTMLButtonElement>>('searchButton');
  private readonly bellButton = viewChild.required<ElementRef<HTMLButtonElement>>('bell');

  readonly menuRequested = output();

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );
  protected readonly crumb = computed(() => findMenuEntry(this.url()));
  protected readonly theme = this.preferences.theme;
  protected readonly unreadCount = inject(NotificationService).unreadCount;
  protected readonly notificationsOpen = signal(false);
  /** Recherche et notifications sont chargées à la demande (@defer) : bundle initial allégé. */
  protected readonly searchOpen = signal(false);
  protected readonly today = computed(() =>
    new Intl.DateTimeFormat(this.language.current(), {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    }).format(new Date()),
  );

  protected logout(): void {
    this.authService.logout().subscribe(() => void this.router.navigate(['/login']));
  }

  protected toggleTheme(): void {
    this.preferences.toggleTheme();
  }

  protected openSearch(): void {
    this.searchOpen.set(true);
  }

  protected closeSearch(): void {
    this.searchOpen.set(false);
    this.searchButton().nativeElement.focus();
  }

  protected toggleNotifications(): void {
    this.notificationsOpen.update((open) => !open);
  }

  protected closeNotifications(): void {
    this.notificationsOpen.set(false);
    this.bellButton().nativeElement.focus();
  }

  /** Ctrl K / ⌘ K ouvre la recherche partout ; « / » aussi, sauf pendant une saisie. */
  protected onShortcut(event: KeyboardEvent): void {
    const typing =
      event.target instanceof HTMLElement && event.target.closest('input, textarea, select');
    const commandK = (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k';
    if (commandK || (event.key === '/' && !typing)) {
      event.preventDefault();
      this.openSearch();
    }
  }
}
