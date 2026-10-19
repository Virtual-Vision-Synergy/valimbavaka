import { ROLE_PERMISSIONS } from '../auth/auth-testing';
import { visibleSections } from './menu';

const labelsFor = (role: string): string[] =>
  visibleSections((permission) => ROLE_PERMISSIONS[role].includes(permission)).flatMap((section) =>
    section.items.map((item) => item.labelKey),
  );

describe('visibleSections', () => {
  it('should show every module to the administrator', () => {
    const labels = labelsFor('ADMIN');

    expect(labels).toContain('menu.item.users');
    expect(labels).toContain('menu.item.suppliers');
    expect(labels).toContain('menu.item.reports');
    expect(labels).toContain('menu.item.journal');
  });

  it('should show sales and catalogue modules but no administration to a seller', () => {
    const labels = labelsFor('SELLER');

    expect(labels).toEqual(
      expect.arrayContaining([
        'menu.item.dashboard',
        'menu.item.articles',
        'menu.item.stocks',
        'menu.item.sales',
        'menu.item.commissions',
      ]),
    );
    expect(labels).not.toContain('menu.item.users');
    expect(labels).not.toContain('menu.item.journal');
    expect(labels).not.toContain('menu.item.deliveries');
    expect(labels).not.toContain('menu.item.production');
  });

  it('should show only the dashboard and deliveries to a delivery driver', () => {
    expect(labelsFor('DELIVERY_DRIVER')).toEqual(['menu.item.dashboard', 'menu.item.deliveries']);
  });

  it('should drop sections that have no allowed item', () => {
    const titles = visibleSections((permission) => permission === 'deliveries.read').map(
      (section) => section.titleKey,
    );

    expect(titles).toEqual(['menu.section.commerce']);
  });
});
