/** Entrée de menu : visible seulement si l'utilisateur a la permission (même code que `@HasPermission` du backend). */
export interface MenuItem {
  readonly label: string;
  readonly route: string;
  readonly permission: string;
}

export interface MenuSection {
  readonly title: string;
  readonly items: readonly MenuItem[];
}

export const MENU_SECTIONS: readonly MenuSection[] = [
  {
    title: 'Principal',
    items: [{ label: 'Tableau de bord', route: '/dashboard', permission: 'dashboard.read' }],
  },
  {
    title: 'Catalogue',
    items: [
      { label: 'Fournisseurs', route: '/suppliers', permission: 'suppliers.read' },
      { label: 'Articles', route: '/articles', permission: 'articles.read' },
    ],
  },
  {
    title: 'Opérations',
    items: [
      { label: 'Stocks', route: '/stocks', permission: 'stocks.read' },
      { label: 'Production', route: '/production', permission: 'production.read' },
      { label: 'Transferts', route: '/transfers', permission: 'transfers.read' },
      { label: 'Inventaire', route: '/inventory', permission: 'inventory.read' },
    ],
  },
  {
    title: 'Commerce',
    items: [
      { label: 'Ventes', route: '/sales', permission: 'sales.read' },
      { label: 'Livraisons', route: '/deliveries', permission: 'deliveries.read' },
      { label: 'Promotions', route: '/promotions', permission: 'promotions.read' },
      { label: 'Commissions', route: '/commissions', permission: 'commissions.read' },
      { label: 'Services', route: '/services', permission: 'services.read' },
    ],
  },
  {
    title: 'Analyse',
    items: [{ label: 'Rapports', route: '/reports', permission: 'reports.read' }],
  },
  {
    title: 'Administration',
    items: [{ label: 'Utilisateurs', route: '/users', permission: 'users.read' }],
  },
];

/** Sections réduites aux entrées autorisées ; une section vide disparaît. */
export function visibleSections(hasPermission: (permission: string) => boolean): MenuSection[] {
  return MENU_SECTIONS.map((section) => ({
    title: section.title,
    items: section.items.filter((item) => hasPermission(item.permission)),
  })).filter((section) => section.items.length > 0);
}
