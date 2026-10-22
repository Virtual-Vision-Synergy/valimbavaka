import { Injectable, computed, signal } from '@angular/core';

export type NotificationKind = 'stock' | 'delivery' | 'day' | 'sync';

/** Notification affichée dans la cloche ; `messageKey` + `params` sont traduits à l'affichage. */
export interface NotificationRDto {
  readonly id: string;
  readonly kind: NotificationKind;
  readonly messageKey: string;
  readonly params: Readonly<Record<string, string | number>>;
  readonly route: string;
  readonly minutesAgo: number;
  readonly read: boolean;
}

/** Données de démonstration tant que le module de notifications n'existe pas côté backend. */
const DEMO_NOTIFICATIONS: readonly NotificationRDto[] = [
  {
    id: 'n-1',
    kind: 'stock',
    messageKey: 'notifications.item.stockOut',
    params: { article: 'Huile de coco vierge 250 ml' },
    route: '/stocks',
    minutesAgo: 4,
    read: false,
  },
  {
    id: 'n-2',
    kind: 'delivery',
    messageKey: 'notifications.item.deliveryFailed',
    params: { reference: 'LIV-0315', driver: 'Tojo N.' },
    route: '/deliveries',
    minutesAgo: 14,
    read: false,
  },
  {
    id: 'n-3',
    kind: 'stock',
    messageKey: 'notifications.item.lowStock',
    params: { count: 6 },
    route: '/stocks',
    minutesAgo: 32,
    read: false,
  },
  {
    id: 'n-4',
    kind: 'day',
    messageKey: 'notifications.item.closingSoon',
    params: { time: '19:00' },
    route: '/day/close',
    minutesAgo: 55,
    read: false,
  },
];

/** Notifications du compte (niveau 2 : store de feature à signaux, §10.5). */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly items = signal<readonly NotificationRDto[]>(DEMO_NOTIFICATIONS);

  readonly notifications = this.items.asReadonly();
  readonly unreadCount = computed(() => this.items().filter((item) => !item.read).length);

  markAllAsRead(): void {
    this.items.update((items) => items.map((item) => ({ ...item, read: true })));
  }
}
