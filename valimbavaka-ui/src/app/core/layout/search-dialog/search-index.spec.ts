import { buildGroups, highlightParts, SearchContext } from './search-index';

const context = (permissions: readonly string[]): SearchContext => ({
  hasPermission: (permission) => permissions.includes(permission),
  t: (key) => key,
});

describe('search index', () => {
  it('buildGroups_shortTerm_returnsNothing', () => {
    expect(buildGroups('s', context(['sales.read']))).toEqual([]);
  });

  it('buildGroups_articleTerm_groupsArticlesBeforeActions', () => {
    const groups = buildGroups('savon', context(['sales.read', 'articles.create']));

    const keys = groups.map((group) => group.key);
    expect(keys[0]).toBe('articles');
    expect(keys).toContain('actions');
    expect(groups[0].items.every((item) => item.title.toLowerCase().includes('savon'))).toBe(true);
  });

  it('buildGroups_withoutPermissions_offersNoAction', () => {
    const groups = buildGroups('savon', context([]));

    expect(groups.some((group) => group.key === 'actions')).toBe(false);
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
