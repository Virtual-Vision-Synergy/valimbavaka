import fr from '../../../../public/i18n/fr.json';
import mg from '../../../../public/i18n/mg.json';

const flatten = (value: unknown, prefix = ''): string[] =>
  typeof value === 'object' && value !== null
    ? Object.entries(value).flatMap(([key, child]) =>
        flatten(child, prefix ? `${prefix}.${key}` : key),
      )
    : [prefix];

describe('translations', () => {
  it('should define every French key in Malagasy and no more', () => {
    expect(flatten(mg).sort()).toEqual(flatten(fr).sort());
  });

  it('should not leave any translation empty', () => {
    const empty = (dictionary: unknown): boolean => JSON.stringify(dictionary).includes('""');

    expect(empty(fr) || empty(mg)).toBe(false);
  });
});
