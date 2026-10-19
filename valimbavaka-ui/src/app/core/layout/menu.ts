/** Entrée de menu : visible seulement si l'utilisateur a la permission (même code que `@HasPermission` du backend). */
export interface MenuItem {
  /** Clé de traduction (`menu.item.*`). */
  readonly labelKey: string;
  readonly route: string;
  readonly permission: string;
}

export interface MenuSection {
  readonly titleKey: string;
  readonly items: readonly MenuItem[];
}

export const MENU_SECTIONS: readonly MenuSection[] = [
  {
    titleKey: 'menu.section.main',
    items: [{ labelKey: 'menu.item.dashboard', route: '/dashboard', permission: 'dashboard.read' }],
  },
  {
    titleKey: 'menu.section.catalog',
    items: [
      { labelKey: 'menu.item.suppliers', route: '/suppliers', permission: 'suppliers.read' },
      { labelKey: 'menu.item.articles', route: '/articles', permission: 'articles.read' },
    ],
  },
  {
    titleKey: 'menu.section.operations',
    items: [
      { labelKey: 'menu.item.stocks', route: '/stocks', permission: 'stocks.read' },
      { labelKey: 'menu.item.production', route: '/production', permission: 'production.read' },
      { labelKey: 'menu.item.transfers', route: '/transfers', permission: 'transfers.read' },
      { labelKey: 'menu.item.inventory', route: '/inventory', permission: 'inventory.read' },
    ],
  },
  {
    titleKey: 'menu.section.commerce',
    items: [
      { labelKey: 'menu.item.sales', route: '/sales', permission: 'sales.read' },
      { labelKey: 'menu.item.deliveries', route: '/deliveries', permission: 'deliveries.read' },
      { labelKey: 'menu.item.promotions', route: '/promotions', permission: 'promotions.read' },
      { labelKey: 'menu.item.commissions', route: '/commissions', permission: 'commissions.read' },
      { labelKey: 'menu.item.services', route: '/services', permission: 'services.read' },
    ],
  },
  {
    titleKey: 'menu.section.analysis',
    items: [{ labelKey: 'menu.item.reports', route: '/reports', permission: 'reports.read' }],
  },
  {
    titleKey: 'menu.section.administration',
    items: [
      { labelKey: 'menu.item.users', route: '/users', permission: 'users.read' },
      { labelKey: 'menu.item.journal', route: '/journal', permission: 'journal.read' },
    ],
  },
];

/** Sections réduites aux entrées autorisées ; une section vide disparaît. */
export function visibleSections(hasPermission: (permission: string) => boolean): MenuSection[] {
  return MENU_SECTIONS.map((section) => ({
    titleKey: section.titleKey,
    items: section.items.filter((item) => hasPermission(item.permission)),
  })).filter((section) => section.items.length > 0);
}
