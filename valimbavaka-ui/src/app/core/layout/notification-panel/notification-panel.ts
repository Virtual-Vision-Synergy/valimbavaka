import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  afterNextRender,
  inject,
  output,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { CalendarClock, PackageX, RefreshCw, Truck } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { RelativeTimePipe } from '../../../shared/pipes/relative-time-pipe';
import { UnderConstruction } from '../../../shared/components/under-construction/under-construction';
import { NotificationKind, NotificationService } from './notification-service';
import { Icon } from '../../../shared/components/icon/icon';

const ICONS = {
  stock: PackageX,
  delivery: Truck,
  day: CalendarClock,
  sync: RefreshCw,
} satisfies Record<NotificationKind, unknown>;

/** Panneau déroulant de la cloche : Échap ou clic extérieur le referment, le focus revient à la cloche. */
@Component({
  selector: 'vvs-notification-panel',
  imports: [RouterLink, TranslatePipe, RelativeTimePipe, UnderConstruction, Icon],
  templateUrl: './notification-panel.html',
  styleUrl: './notification-panel.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(keydown.escape)': 'closed.emit()',
    '(document:click)': 'onDocumentClick($event)',
  },
})
export class NotificationPanel {
  private readonly service = inject(NotificationService);
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly closed = output();

  protected readonly notifications = this.service.notifications;
  protected readonly unreadCount = this.service.unreadCount;
  protected readonly icons = ICONS;

  constructor() {
    afterNextRender(() => this.element.nativeElement.querySelector<HTMLElement>('h2')?.focus());
  }

  protected markAllAsRead(): void {
    this.service.markAllAsRead();
  }

  protected onDocumentClick(event: MouseEvent): void {
    const target = event.target as Node | null;
    const insideNotif = target instanceof Element && target.closest('.notif');
    if (!insideNotif) {
      this.closed.emit();
    }
  }
}
