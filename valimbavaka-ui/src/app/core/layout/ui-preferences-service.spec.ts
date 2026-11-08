import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { UiPreferencesService } from './ui-preferences-service';

describe('UiPreferencesService', () => {
  const html = document.documentElement;
  const view = document.defaultView as Window;

  const stubSystemTheme = (dark: boolean): void => {
    Object.defineProperty(view, 'matchMedia', {
      configurable: true,
      writable: true,
      value: (query: string) => ({ matches: dark && query === '(prefers-color-scheme: dark)' }),
    });
  };

  const otherTabWrites = (key: string, newValue: string | null): void => {
    view.dispatchEvent(new StorageEvent('storage', { key, newValue }));
  };

  beforeEach(() => {
    localStorage.clear();
    html.classList.remove('dark');
  });

  afterEach(() => {
    Reflect.deleteProperty(view, 'matchMedia');
    vi.restoreAllMocks();
    html.classList.remove('dark');
    localStorage.clear();
  });

  it('should start in light theme when nothing is stored and the system is light', () => {
    stubSystemTheme(false);

    const sut = TestBed.inject(UiPreferencesService);

    expect(sut.theme()).toBe('light');
    expect(html.classList.contains('dark')).toBe(false);
  });

  it('should follow the dark system preference when nothing is stored', () => {
    stubSystemTheme(true);

    const sut = TestBed.inject(UiPreferencesService);

    expect(sut.theme()).toBe('dark');
    expect(html.classList.contains('dark')).toBe(true);
  });

  it('should prefer the remembered choice over the system preference', () => {
    stubSystemTheme(true);
    localStorage.setItem('vvs.theme', 'light');

    const sut = TestBed.inject(UiPreferencesService);

    expect(sut.theme()).toBe('light');
    expect(html.classList.contains('dark')).toBe(false);
  });

  it('should ignore an unsupported remembered value and fall back to the system', () => {
    stubSystemTheme(false);
    localStorage.setItem('vvs.theme', 'blue');

    const sut = TestBed.inject(UiPreferencesService);

    expect(sut.theme()).toBe('light');
  });

  it('should switch the whole interface to dark at once and remember the choice', () => {
    stubSystemTheme(false);
    const sut = TestBed.inject(UiPreferencesService);

    sut.toggleTheme();

    expect(sut.theme()).toBe('dark');
    expect(html.classList.contains('dark')).toBe(true);
    expect(localStorage.getItem('vvs.theme')).toBe('dark');
  });

  it('should switch back to light on a second toggle', () => {
    stubSystemTheme(false);
    const sut = TestBed.inject(UiPreferencesService);

    sut.toggleTheme();
    sut.toggleTheme();

    expect(sut.theme()).toBe('light');
    expect(html.classList.contains('dark')).toBe(false);
    expect(localStorage.getItem('vvs.theme')).toBe('light');
  });

  it('should still switch the theme when the browser storage is unavailable', () => {
    stubSystemTheme(false);
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('Stockage plein', 'QuotaExceededError');
    });
    const sut = TestBed.inject(UiPreferencesService);

    sut.toggleTheme();

    expect(sut.theme()).toBe('dark');
    expect(html.classList.contains('dark')).toBe(true);
  });

  it('should apply a theme chosen in another tab without writing it again', () => {
    stubSystemTheme(false);
    const sut = TestBed.inject(UiPreferencesService);
    const setItem = vi.spyOn(Storage.prototype, 'setItem');

    otherTabWrites('vvs.theme', 'dark');

    expect(sut.theme()).toBe('dark');
    expect(html.classList.contains('dark')).toBe(true);
    expect(setItem).not.toHaveBeenCalled();
  });

  it('should ignore another tab writing another key or an unsupported theme', () => {
    stubSystemTheme(false);
    const sut = TestBed.inject(UiPreferencesService);

    otherTabWrites('vvs.lang', 'dark');
    otherTabWrites('vvs.theme', 'blue');
    otherTabWrites('vvs.theme', null);

    expect(sut.theme()).toBe('light');
    expect(html.classList.contains('dark')).toBe(false);
  });

  it('should stop following other tabs once the application is destroyed', () => {
    stubSystemTheme(false);
    TestBed.inject(UiPreferencesService);

    TestBed.resetTestingModule();
    otherTabWrites('vvs.theme', 'dark');

    expect(html.classList.contains('dark')).toBe(false);
  });
});
