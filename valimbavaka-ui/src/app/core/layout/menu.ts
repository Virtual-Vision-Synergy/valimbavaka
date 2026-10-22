import {
  ArrowLeftRight,
  ChartColumn,
  ClipboardList,
  Factory,
  HandCoins,
  Handshake,
  LayoutDashboard,
  Lock,
  Package,
  RotateCcwClock,
  Scissors,
  ShoppingCart,
  Store,
  Sunrise,
  Tag,
  Truck,
  Users,
  Warehouse,
  IconNode,
} from 'lucide';

/** Entrée de menu : visible seulement si l'utilisateur a la permission (même code que `@HasPermission` du backend). */
export interface MenuItem {
  /** Clé de traduction (`menu.item.*`). */
  readonly labelKey: string;
  readonly route: string;
  readonly permission: string;
  readonly icon: IconNode;
}

export interface MenuSection {
  readonly titleKey: string;
  readonly items: readonly MenuItem[];
}

export const MENU_SECTIONS: readonly MenuSection[] = [
  {
    titleKey: 'menu.section.main',
    items: [
      {
        labelKey: 'menu.item.dashboard',
        route: '/dashboard',
        permission: 'dashboard.read',
        icon: LayoutDashboard,
      },
    ],
  },
  {
    titleKey: 'menu.section.day',
    items: [
      {
        labelKey: 'menu.item.dayOpen',
        route: '/day/open',
        permission: 'sales.read',
        icon: Sunrise,
      },
      {
        labelKey: 'menu.item.dayClose',
        route: '/day/close',
        permission: 'sales.read',
        icon: Lock,
      },
    ],
  },
  {
    titleKey: 'menu.section.catalog',
    items: [
      {
        labelKey: 'menu.item.suppliers',
        route: '/suppliers',
        permission: 'suppliers.read',
        icon: Handshake,
      },
      {
        labelKey: 'menu.item.articles',
        route: '/articles',
        permission: 'articles.read',
        icon: Package,
      },
    ],
  },
  {
    titleKey: 'menu.section.operations',
    items: [
      {
        labelKey: 'menu.item.stocks',
        route: '/stocks',
        permission: 'stocks.read',
        icon: Warehouse,
      },
      {
        labelKey: 'menu.item.production',
        route: '/production',
        permission: 'production.read',
        icon: Factory,
      },
      {
        labelKey: 'menu.item.transfers',
        route: '/transfers',
        permission: 'transfers.read',
        icon: ArrowLeftRight,
      },
      {
        labelKey: 'menu.item.inventory',
        route: '/inventory',
        permission: 'inventory.read',
        icon: ClipboardList,
      },
    ],
  },
  {
    titleKey: 'menu.section.commerce',
    items: [
      {
        labelKey: 'menu.item.pos',
        route: '/pos',
        permission: 'sales.read',
        icon: Store,
      },
      {
        labelKey: 'menu.item.sales',
        route: '/sales',
        permission: 'sales.read',
        icon: ShoppingCart,
      },
      {
        labelKey: 'menu.item.deliveries',
        route: '/deliveries',
        permission: 'deliveries.read',
        icon: Truck,
      },
      {
        labelKey: 'menu.item.promotions',
        route: '/promotions',
        permission: 'promotions.read',
        icon: Tag,
      },
      {
        labelKey: 'menu.item.commissions',
        route: '/commissions',
        permission: 'commissions.read',
        icon: HandCoins,
      },
      {
        labelKey: 'menu.item.services',
        route: '/services',
        permission: 'services.read',
        icon: Scissors,
      },
    ],
  },
  {
    titleKey: 'menu.section.analysis',
    items: [
      {
        labelKey: 'menu.item.reports',
        route: '/reports',
        permission: 'reports.read',
        icon: ChartColumn,
      },
      {
        labelKey: 'menu.item.journal',
        route: '/journal',
        permission: 'journal.read',
        icon: RotateCcwClock,
      },
    ],
  },
  {
    titleKey: 'menu.section.administration',
    items: [
      {
        labelKey: 'menu.item.users',
        route: '/users',
        permission: 'users.read',
        icon: Users,
      },
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

/** Section et entrée de menu correspondant à une URL (fil d'Ariane) ; la route la plus longue l'emporte. */
export function findMenuEntry(url: string): { section: MenuSection; item: MenuItem } | undefined {
  const path = url.split(/[?#]/)[0];
  return MENU_SECTIONS.flatMap((section) => section.items.map((item) => ({ section, item })))
    .filter(({ item }) => path === item.route || path.startsWith(`${item.route}/`))
    .sort((a, b) => b.item.route.length - a.item.route.length)[0];
}
