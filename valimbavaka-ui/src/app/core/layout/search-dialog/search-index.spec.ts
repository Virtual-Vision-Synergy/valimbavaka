import { ArticleRDto } from '../../../features/article/data-access/article-models';
import { ROLE_PERMISSIONS } from '../../auth/auth-testing';
import fr from '../../../../../public/i18n/fr.json';
import { buildGroups, highlightParts, normalizeTerm, SearchContext } from './search-index';

/** Traduction minimale : lit la clé dans fr.json et remplace les paramètres `{{x}}`. */
function translate(key: string, params: object = {}): string {
  const value = key.split('.').reduce<unknown>((node, part) => {
    return node && typeof node === 'object' ? (node as Record<string, unknown>)[part] : undefined;
  }, fr);
  const text = typeof value === 'string' ? value : key;
  return Object.entries(params).reduce(
    (result, [name, param]) => result.replace(`{{${name}}}`, String(param)),
    text,
  );
}

const context = (permissions: readonly string[]): SearchContext => ({
  hasPermission: (permission) => permissions.includes(permission),
  t: translate,
});

const admin = context(ROLE_PERMISSIONS['ADMIN']);

const article = (overrides: Partial<ArticleRDto>): ArticleRDto => ({
  id: '0195f0a0-0000-7000-8000-0000000000a1',
  code: 'ART-0031',
  name: 'Savon noir 100 g',
  barcode: '6201234567890',
  type: 'FINISHED',
  rangeId: null,
  unit: 'PIECE',
  cost: 2500,
  price: 12000,
  threshold: 10,
  supplierId: null,
  active: true,
  createdAt: '2026-10-26T08:00:00Z',
  updatedAt: '2026-10-26T08:00:00Z',
  version: 0,
  ...overrides,
});

const savon = article({});
const beurre = article({
  id: '0195f0a0-0000-7000-8000-0000000000a2',
  code: 'ART-0045',
  name: 'Beurre de karité brut',
  barcode: null,
  type: 'RAW_MATERIAL',
  price: null,
});

const titles = (term: string, ctx: SearchContext, articles: readonly ArticleRDto[] = []) =>
  buildGroups(term, ctx, articles).map((group) => ({
    key: group.key,
    titles: group.items.map((item) => item.title),
  }));

describe('search index', () => {
  it('normalizeTerm_oneCharacterAfterTrim_returnsEmpty', () => {
    expect(normalizeTerm('  s  ')).toBe('');
    expect(normalizeTerm(' SA ')).toBe('sa');
  });

  it('buildGroups_shortTerm_returnsNothing', () => {
    expect(buildGroups('s', admin, [savon])).toEqual([]);
  });

  it('buildGroups_articleTerm_listsServerArticlesWithCodeAndPrice', () => {
    const groups = buildGroups('savon', admin, [savon]);

    expect(groups[0].key).toBe('articles');
    expect(groups[0].items[0]).toMatchObject({
      title: 'Savon noir 100 g',
      meta: `ART-0031 · ${new Intl.NumberFormat('fr-FR').format(12000)} Ar`,
      aside: 'Produit fini',
      route: `/articles/${savon.id}`,
    });
  });

  it('buildGroups_articleWithoutPrice_showsCodeOnly', () => {
    const groups = buildGroups('karité', admin, [beurre]);

    expect(groups[0].items[0]).toMatchObject({ meta: 'ART-0045', aside: 'Matière première' });
  });

  it('buildGroups_codeOrBarcodeTerm_keepsTheArticle', () => {
    expect(titles('art-0031', admin, [savon, beurre])[0].titles).toEqual(['Savon noir 100 g']);
    expect(titles('62012345', admin, [savon, beurre])[0].titles).toEqual(['Savon noir 100 g']);
  });

  it('buildGroups_staleServerPage_dropsArticlesThatNoLongerMatch', () => {
    const groups = buildGroups('savon n', admin, [savon, beurre]);

    expect(groups[0].items.map((item) => item.title)).toEqual(['Savon noir 100 g']);
  });

  it('buildGroups_withoutArticlesRead_hidesArticles', () => {
    const driver = context(ROLE_PERMISSIONS['DELIVERY_DRIVER']);

    expect(buildGroups('savon', driver, [savon])).toEqual([]);
  });

  it('buildGroups_creationTerm_filtersCreateActionsByLabel', () => {
    const groups = titles('créer un f', admin);

    expect(groups).toEqual([{ key: 'actions', titles: ['Créer un fournisseur'] }]);
  });

  it('buildGroups_sellerTypingCreer_offersNoAdminCreation', () => {
    expect(titles('créer', context(ROLE_PERMISSIONS['SELLER']))).toEqual([]);
  });

  it('buildGroups_navigationTerm_listsAllowedPagesOnly', () => {
    expect(titles('fourn', admin)).toContainEqual({
      key: 'goto',
      titles: ['Catalogue › Fournisseurs'],
    });
    expect(titles('fourn', context(ROLE_PERMISSIONS['SELLER']))).toEqual([]);
  });

  it('buildGroups_mixedTerm_ordersArticlesThenActionsThenPages', () => {
    const keys = buildGroups('article', admin, [article({ name: 'Article test' })]).map(
      (group) => group.key,
    );

    expect(keys).toEqual(['articles', 'actions', 'goto']);
  });

  it('highlightParts_matchInside_splitsAroundTheMatch', () => {
    expect(highlightParts('Savon noir', 'avo')).toEqual([
      { text: 'S', match: false },
      { text: 'avo', match: true },
      { text: 'n noir', match: false },
    ]);
  });

  it('highlightParts_noMatch_returnsWholeText', () => {
    expect(highlightParts('Savon', 'xyz')).toEqual([{ text: 'Savon', match: false }]);
  });
});
