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
import { rxResource, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { CornerDownLeft, Search } from 'lucide';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { AuthStore } from '../../auth/auth-store';
import { ArticleService } from '../../../features/article/data-access/article-service';
import { ArticleRDto } from '../../../features/article/data-access/article-models';
import { Icon } from '../../../shared/components/icon/icon';
import { RangeTag } from '../../../shared/components/range-tag/range-tag';
import {
  MAX_ARTICLES,
  SearchItem,
  buildGroups,
  highlightParts,
  normalizeTerm,
} from './search-index';

/** Délai après la dernière frappe avant d'interroger le serveur (même valeur que la liste des articles). */
const SEARCH_DEBOUNCE_MS = 300;

/**
 * Recherche globale (SYS-03, Ctrl K) : combobox + listbox dans un <dialog> modal natif
 * (focus piégé et Échap fournis par le navigateur), flèches pour parcourir, Entrée pour ouvrir.
 * Pages et créations sont filtrées à chaque frappe ; les articles viennent de `GET /api/v1/articles`
 * (ART-01), demandés 300 ms après la dernière frappe et refiltrés localement sur la saisie courante.
 */
@Component({
  selector: 'vvs-search-dialog',
  imports: [TranslatePipe, Icon, RangeTag],
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
  private readonly articleService = inject(ArticleService);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  protected readonly query = signal('');
  protected readonly term = computed(() => this.query().trim());
  /** Saisie envoyée au serveur : normalisée et temporisée pour ne pas interroger à chaque frappe. */
  private readonly search = toSignal(
    toObservable(computed(() => normalizeTerm(this.query()))).pipe(
      debounceTime(SEARCH_DEBOUNCE_MS),
      distinctUntilChanged(),
    ),
    { initialValue: '' },
  );
  /** Une page d'articles (nom, code, code-barres) ; aucun appel sans saisie ni sans `articles.read`. */
  protected readonly articles = rxResource({
    params: () =>
      this.search() && this.auth.hasPermission('articles.read') ? this.search() : undefined,
    stream: ({ params }) =>
      this.articleService.list({
        page: 1,
        size: MAX_ARTICLES,
        sortBy: 'name',
        sortDirection: 'ASC',
        search: params,
      }),
  });
  /** `value()` lève une exception tant que la ressource est en erreur : toujours passer par `hasValue()`. */
  private readonly articleRows = computed<readonly ArticleRDto[]>(() =>
    this.articles.hasValue() ? this.articles.value().items : [],
  );
  protected readonly groups = computed(() =>
    buildGroups(
      this.query(),
      {
        hasPermission: (permission) => this.auth.hasPermission(permission),
        t: (key, params) => this.translate.instant(key, params),
      },
      this.articleRows(),
    ),
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
    afterNextRender(() => {
      const element = this.dialog().nativeElement;
      if (typeof element.showModal === 'function') {
        element.showModal();
      } else {
        element.setAttribute('open', '');
      }
    });
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
    const element = this.dialog().nativeElement;
    if (typeof element.close === 'function') {
      element.close();
    } else {
      // Environnement sans <dialog> natif (tests) : l'événement `close` ne viendra pas.
      element.removeAttribute('open');
      this.closed.emit();
    }
  }

  /** Clic sur le voile (hors de la boîte) : referme. */
  protected onBackdropClick(event: MouseEvent): void {
    if (event.target === this.dialog().nativeElement) {
      this.close();
    }
  }
}
