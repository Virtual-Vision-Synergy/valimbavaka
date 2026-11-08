import { DOCUMENT } from '@angular/common';
import { DestroyRef, Injectable, inject, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

const THEME_KEY = 'vvs.theme';
const SIDEBAR_KEY = 'vvs.sidebar-collapsed';

function isTheme(value: string | null): value is Theme {
  return value === 'light' || value === 'dark';
}

/**
 * Préférences d'affichage (thème, menu replié) : aucune donnée sensible, mémorisées dans le navigateur.
 * Sans choix explicite, le thème suit le système (prefers-color-scheme).
 * Le thème est porté par la classe `dark` de <html> : toute l'interface bascule d'un coup, sans rechargement,
 * y compris dans les autres onglets ouverts (événement `storage`) (SYS-04).
 */
@Injectable({ providedIn: 'root' })
export class UiPreferencesService {
  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);

  private readonly selectedTheme = signal<Theme>(this.initialTheme());
  private readonly collapsed = signal(this.read(SIDEBAR_KEY) === 'true');

  readonly theme = this.selectedTheme.asReadonly();
  readonly sidebarCollapsed = this.collapsed.asReadonly();

  constructor() {
    this.applyTheme(this.selectedTheme());
    this.followOtherTabs();
  }

  toggleTheme(): void {
    const next: Theme = this.selectedTheme() === 'dark' ? 'light' : 'dark';
    this.selectedTheme.set(next);
    this.write(THEME_KEY, next);
    this.applyTheme(next);
  }

  toggleSidebar(): void {
    this.collapsed.update((value) => !value);
    this.write(SIDEBAR_KEY, String(this.collapsed()));
  }

  private applyTheme(theme: Theme): void {
    this.document.documentElement.classList.toggle('dark', theme === 'dark');
  }

  /** Un choix fait dans un autre onglet s'applique aussi à celui-ci, sans le mémoriser une seconde fois. */
  private followOtherTabs(): void {
    const view = this.document.defaultView;
    const listener = (event: StorageEvent): void => {
      if (event.key === THEME_KEY && isTheme(event.newValue)) {
        this.selectedTheme.set(event.newValue);
        this.applyTheme(event.newValue);
      }
    };
    view?.addEventListener('storage', listener);
    this.destroyRef.onDestroy(() => view?.removeEventListener('storage', listener));
  }

  private initialTheme(): Theme {
    const stored = this.read(THEME_KEY);
    if (isTheme(stored)) {
      return stored;
    }
    const prefersDark = this.document.defaultView?.matchMedia?.(
      '(prefers-color-scheme: dark)',
    ).matches;
    return prefersDark ? 'dark' : 'light';
  }

  private read(key: string): string | null {
    try {
      // eslint-disable-next-line no-restricted-globals -- préférence d'affichage, aucune donnée sensible (§9)
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  }

  private write(key: string, value: string): void {
    try {
      // eslint-disable-next-line no-restricted-globals -- préférence d'affichage, aucune donnée sensible (§9)
      localStorage.setItem(key, value);
    } catch {
      // Stockage indisponible (navigation privée) : la préférence vaut pour la session seulement.
    }
  }
}
