import { IconNode, Handshake, Package, Plus, ShoppingCart, Truck } from 'lucide';
import { ARTICLES_FIXTURE } from '../../../features/articles/data-access/article-fixtures';
import { ProductRange } from '../../../shared/models/tone';
import { MENU_SECTIONS } from '../menu';

export type SearchGroupKey = 'articles' | 'records' | 'actions' | 'goto';

export interface SearchItem {
  readonly id: string;
  readonly icon: IconNode;
  readonly title: string;
  readonly meta: string;
  readonly range: ProductRange | null;
  /** Libellé discret à droite (type d'enregistrement). */
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

interface RecordEntry {
  readonly id: string;
  readonly title: string;
  readonly reference: string;
  readonly typeKey: string;
  readonly icon: IconNode;
  readonly route: string;
}

/** Ventes, livraisons et fournisseurs de démonstration en attendant l'API de recherche. */
const RECORDS: readonly RecordEntry[] = [
  {
    id: 's1',
    title: 'Vente Miora A. · 54 000 Ar',
    reference: 'VTE-2451',
    typeKey: 'search.type.sale',
    icon: ShoppingCart,
    route: '/sales',
  },
  {
    id: 'd1',
    title: 'Livraison Analakely',
    reference: 'LIV-0318',
    typeKey: 'search.type.delivery',
    icon: Truck,
    route: '/deliveries',
  },
  {
    id: 'f1',
    title: 'Huilerie Sambava',
    reference: 'FRS-0012',
    typeKey: 'search.type.supplier',
    icon: Handshake,
    route: '/suppliers',
  },
];

const MAX_ARTICLES = 5;

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

function articleItems(term: string, ctx: SearchContext): readonly SearchItem[] {
  return ARTICLES_FIXTURE.filter((a) => matches(`${a.name} ${a.reference}`, term))
    .slice(0, MAX_ARTICLES)
    .map((a) => ({
      id: `article-${a.id}`,
      icon: Package,
      title: a.name,
      meta: ctx.t('search.articleMeta', {
        reference: a.reference,
        stock: a.totalStock,
        price: a.price,
      }),
      range: a.range,
      aside: a.range ? '' : ctx.t('search.type.material'),
      route: `/articles/${a.id}`,
      highlight: true,
    }));
}

function recordItems(term: string, ctx: SearchContext): readonly SearchItem[] {
  return RECORDS.filter((r) => matches(`${r.title} ${r.reference}`, term)).map((r) => ({
    id: r.id,
    icon: r.icon,
    title: r.title,
    meta: r.reference,
    range: null,
    aside: ctx.t(r.typeKey),
    route: r.route,
    highlight: true,
  }));
}

function action(id: string, title: string, route: string): SearchItem {
  return { id, icon: Plus, title, meta: '', range: null, aside: '', route, highlight: false };
}

function actionItems(articles: readonly SearchItem[], ctx: SearchContext): readonly SearchItem[] {
  const items: SearchItem[] = [];
  if (articles.length > 0 && ctx.hasPermission('sales.read')) {
    const title = ctx.t('search.action.newSale', { name: articles[0].title });
    items.push(action('new-sale', title, '/pos'));
  }
  if (ctx.hasPermission('articles.create')) {
    items.push(action('new-article', ctx.t('search.action.createArticle'), '/articles/new'));
  }
  return items;
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

/** Résultats groupés pour une saisie (≥ 2 caractères) ; les groupes vides sont omis. */
export function buildGroups(rawTerm: string, ctx: SearchContext): readonly SearchGroup[] {
  const term = rawTerm.trim().toLowerCase();
  if (term.length < 2) {
    return [];
  }
  const articles = articleItems(term, ctx);
  const groups: SearchGroup[] = [
    { key: 'articles', items: articles },
    { key: 'records', items: recordItems(term, ctx) },
    { key: 'actions', items: actionItems(articles, ctx) },
    { key: 'goto', items: gotoItems(term, ctx) },
  ];
  return groups.filter((group) => group.items.length > 0);
}
