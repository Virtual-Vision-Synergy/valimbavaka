import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  afterNextRender,
  computed,
  inject,
  linkedSignal,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { Router } from '@angular/router';
import { CornerDownLeft, Search } from 'lucide';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthStore } from '../../auth/auth-store';
import { Icon } from '../../../shared/components/icon/icon';
import { RangeTag } from '../../../shared/components/range-tag/range-tag';
import { UnderConstruction } from '../../../shared/components/under-construction/under-construction';
import { SearchItem, buildGroups, highlightParts } from './search-index';

/**
 * Recherche globale (SYS-03, Ctrl K) : combobox + listbox dans un <dialog> modal natif
 * (focus piégé et Échap fournis par le navigateur), flèches pour parcourir, Entrée pour ouvrir.
 */
@Component({
  selector: 'vvs-search-dialog',
  imports: [TranslatePipe, UnderConstruction, Icon, RangeTag],
  templateUrl: './search-dialog.html',
  styleUrl: './search-dialog.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  // Voile cliquable à la souris ; au clavier, Échap ferme déjà le <dialog> natif.
  host: { '(click)': 'onBackdropClick($event)' },
})
export class SearchDialog {
  protected readonly icons = { CornerDownLeft, Search };
  private readonly router = inject(Router);
  private readonly auth = inject(AuthStore);
  private readonly translate = inject(TranslateService);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  protected readonly query = signal('');
  protected readonly term = computed(() => this.query().trim());
  protected readonly groups = computed(() =>
    buildGroups(this.query(), {
      hasPermission: (permission) => this.auth.hasPermission(permission),
      t: (key, params) => this.translate.instant(key, params),
    }),
  );
  protected readonly flat = computed(() => this.groups().flatMap((group) => group.items));
  /** Résultat actif : revient au premier quand les résultats changent. */
  protected readonly active = linkedSignal<readonly SearchItem[], number>({
    source: this.flat,
    computation: () => 0,
  });

  /** Émis à la fermeture (bouton, Échap, voile, choix d'un résultat) : le parent retire le dialogue. */
  readonly closed = output();

  constructor() {
    afterNextRender(() => this.dialog().nativeElement.showModal());
  }

  protected parts(text: string): readonly { text: string; match: boolean }[] {
    return highlightParts(text, this.term());
  }

  protected indexOf(item: SearchItem): number {
    return this.flat().indexOf(item);
  }

  protected onKeydown(event: KeyboardEvent): void {
    const count = this.flat().length;
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      const step = event.key === 'ArrowDown' ? 1 : -1;
      this.active.update((index) => (count ? (index + step + count) % count : 0));
      document.getElementById(`search-opt-${this.active()}`)?.scrollIntoView({ block: 'nearest' });
    } else if (event.key === 'Enter' && count) {
      event.preventDefault();
      this.open(this.flat()[this.active()]);
    }
  }

  protected open(item: SearchItem): void {
    this.close();
    void this.router.navigateByUrl(item.route);
  }

  protected close(): void {
    this.dialog().nativeElement.close();
  }

  /** Clic sur le voile (hors de la boîte) : referme. */
  protected onBackdropClick(event: MouseEvent): void {
    if (event.target === this.dialog().nativeElement) {
      this.close();
    }
  }
}
