import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CircleAlert, CircleCheck, Info, TriangleAlert, X } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../icon/icon';
import { ToastService } from './toast-service';

/** Pile de toasts (verre, bas d'écran) ; une seule région `aria-live` pour toute l'application. */
@Component({
  selector: 'vvs-toast-host',
  imports: [TranslatePipe, Icon],
  templateUrl: './toast-host.html',
  styleUrl: './toast-host.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToastHost {
  private readonly toastService = inject(ToastService);

  protected readonly toasts = this.toastService.toasts;
  protected readonly icons = {
    success: CircleCheck,
    info: Info,
    warning: TriangleAlert,
    danger: CircleAlert,
    close: X,
  };

  protected dismiss(id: number): void {
    this.toastService.dismiss(id);
  }
}
