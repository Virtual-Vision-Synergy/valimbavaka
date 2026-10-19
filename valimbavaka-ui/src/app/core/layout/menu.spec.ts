import { ROLE_PERMISSIONS } from '../auth/auth-testing';
import { visibleSections } from './menu';

const labelsFor = (role: string): string[] =>
  visibleSections((permission) => ROLE_PERMISSIONS[role].includes(permission)).flatMap((section) =>
    section.items.map((item) => item.label),
  );

describe('visibleSections', () => {
  it('should show every module to the administrator', () => {
    const labels = labelsFor('ADMIN');

    expect(labels).toContain('Utilisateurs');
    expect(labels).toContain('Fournisseurs');
    expect(labels).toContain('Rapports');
  });

  it('should show sales and catalogue modules but no administration to a seller', () => {
    const labels = labelsFor('SELLER');

    expect(labels).toEqual(
      expect.arrayContaining(['Tableau de bord', 'Articles', 'Stocks', 'Ventes', 'Commissions']),
    );
    expect(labels).not.toContain('Utilisateurs');
    expect(labels).not.toContain('Livraisons');
    expect(labels).not.toContain('Production');
  });

  it('should show only the dashboard and deliveries to a delivery driver', () => {
    expect(labelsFor('DELIVERY_DRIVER')).toEqual(['Tableau de bord', 'Livraisons']);
  });

  it('should drop sections that have no allowed item', () => {
    const titles = visibleSections((permission) => permission === 'deliveries.read').map(
      (section) => section.title,
    );

    expect(titles).toEqual(['Commerce']);
  });
});
