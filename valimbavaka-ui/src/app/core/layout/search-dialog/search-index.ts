import { IconNode, Package, Plus } from 'lucide';
import { ArticleRDto } from '../../../features/article/data-access/article-models';
import { ProductRange } from '../../../shared/models/tone';
import { formatAriary } from '../../../shared/pipes/ariary-pipe';
import { MENU_SECTIONS } from '../menu';

export type SearchGroupKey = 'articles' | 'actions' | 'goto';

/** Saisie minimale (caractères, espaces rognés) avant de chercher. */
export const MIN_TERM_LENGTH = 2;

/** Nombre maximal d'articles proposés par la palette (une seule page demandée au serveur). */
export const MAX_ARTICLES = 5;

export interface SearchItem {
  readonly id: string;
  readonly icon: IconNode;
  readonly title: string;
  readonly meta: string;
  readonly range: ProductRange | null;
  /** Libellé discret à droite (type d'article). */
  readonly aside: string;
  readonly route: string;
  readonly highlight: boolean;
}

export interface SearchGroup {
  readonly key: SearchGroupKey;
  readonly items: readonly SearchItem[];
}

export interface SearchContext {
  readonly hasPermission: (permission: string) => boolean;
  /** Traduit une clé i18n (paramètres facultatifs). */
  readonly t: (key: string, params?: object) => string;
}

/** Action de création : visible seulement avec la permission d'écriture correspondante (mêmes codes que le backend). */
interface CreateAction {
  readonly id: string;
  readonly labelKey: string;
  readonly route: string;
  readonly permission: string;
}

const CREATE_ACTIONS: readonly CreateAction[] = [
  { id: 'new-sale', labelKey: 'search.action.newSale', route: '/pos', permission: 'sales.read' },
  {
    id: 'new-article',
    labelKey: 'search.action.createArticle',
    route: '/articles/new',
    permission: 'articles.create',
  },
  {
    id: 'new-supplier',
    labelKey: 'search.action.createSupplier',
    route: '/suppliers/new',
    permission: 'suppliers.create',
  },
  {
    id: 'new-user',
    labelKey: 'search.action.createUser',
    route: '/users/new',
    permission: 'users.create',
  },
];

/** Saisie normalisée (espaces rognés, minuscules) ; vide si elle est trop courte pour chercher. */
export function normalizeTerm(rawTerm: string): string {
  const term = rawTerm.trim().toLowerCase();
  return term.length < MIN_TERM_LENGTH ? '' : term;
}

export function matches(text: string, term: string): boolean {
  return text.toLowerCase().includes(term);
}

/** Découpe un texte autour de la saisie pour surligner sans `innerHTML`. */
export function highlightParts(
  text: string,
  term: string,
): readonly { text: string; match: boolean }[] {
  const index = term ? text.toLowerCase().indexOf(term.toLowerCase()) : -1;
  if (index < 0) {
    return [{ text, match: false }];
  }
  const end = index + term.length;
  return [
    { text: text.slice(0, index), match: false },
    { text: text.slice(index, end), match: true },
    { text: text.slice(end), match: false },
  ].filter((part) => part.text);
}

/**
 * Articles renvoyés par le serveur (nom, code, code-barres) : filtrés à nouveau sur la saisie courante pour
 * que la liste suive la frappe sans attendre la réponse suivante.
 */
function articleItems(
  articles: readonly ArticleRDto[],
  term: string,
  ctx: SearchContext,
): readonly SearchItem[] {
  return articles
    .filter((a) => matches(`${a.name} ${a.code} ${a.barcode ?? ''}`, term))
    .slice(0, MAX_ARTICLES)
    .map((a) => ({
      id: `article-${a.id}`,
      icon: Package,
      title: a.name,
      meta:
        a.price === null
          ? a.code
          : ctx.t('search.articleMeta', { reference: a.code, price: formatAriary(a.price) }),
      range: null,
      aside: ctx.t(`article.type.${a.type}`),
      route: `/articles/${a.id}`,
      highlight: true,
    }));
}

function actionItems(term: string, ctx: SearchContext): readonly SearchItem[] {
  return CREATE_ACTIONS.filter((action) => ctx.hasPermission(action.permission))
    .map((action) => ({ action, title: ctx.t(action.labelKey) }))
    .filter(({ title }) => matches(title, term))
    .map(({ action, title }) => ({
      id: action.id,
      icon: Plus,
      title,
      meta: '',
      range: null,
      aside: '',
      route: action.route,
      highlight: true,
    }));
}

function gotoItems(term: string, ctx: SearchContext): readonly SearchItem[] {
  return MENU_SECTIONS.flatMap((section) =>
    section.items
      .filter((item) => ctx.hasPermission(item.permission) && matches(ctx.t(item.labelKey), term))
      .map((item) => ({
        id: `goto-${item.route}`,
        icon: item.icon,
        title: `${ctx.t(section.titleKey)} › ${ctx.t(item.labelKey)}`,
        meta: '',
        range: null,
        aside: '',
        route: item.route,
        highlight: false,
      })),
  );
}

/**
 * Résultats groupés pour une saisie (≥ 2 caractères) : articles (lus sur l'API), créations puis pages,
 * filtrés par les permissions de l'utilisateur. Les groupes vides sont omis.
 */
export function buildGroups(
  rawTerm: string,
  ctx: SearchContext,
  articles: readonly ArticleRDto[] = [],
): readonly SearchGroup[] {
  const term = normalizeTerm(rawTerm);
  if (!term) {
    return [];
  }
  const groups: SearchGroup[] = [
    {
      key: 'articles',
      items: ctx.hasPermission('articles.read') ? articleItems(articles, term, ctx) : [],
    },
    { key: 'actions', items: actionItems(term, ctx) },
    { key: 'goto', items: gotoItems(term, ctx) },
  ];
  return groups.filter((group) => group.items.length > 0);
}
