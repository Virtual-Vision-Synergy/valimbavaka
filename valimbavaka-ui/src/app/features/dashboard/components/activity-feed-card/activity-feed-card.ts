import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  ArrowLeftRight,
  ArrowRight,
  Camera,
  Check,
  Scissors,
  ShoppingCart,
  X,
  IconNode,
} from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { RelativeTimePipe } from '../../../../shared/pipes/relative-time-pipe';
import { HasPermissionDirective } from '../../../../core/auth/has-permission-directive';
import { formatAriary } from '../../../../shared/pipes/ariary-pipe';
import { ActivityKind, ActivityRDto } from '../../data-access/dashboard-models';
import { Icon } from '../../../../shared/components/icon/icon';

const ICONS: Record<ActivityKind, IconNode> = {
  SALE: ShoppingCart,
  DELIVERY: Check,
  DELIVERY_FAILED: X,
  SERVICE: Scissors,
  SHOOTING: Camera,
  TRANSFER: ArrowLeftRight,
};

/** Fil d'activité en direct (JR-03) : chaque événement = acteur + action traduite + contexte. */
@Component({
  selector: 'vvs-activity-feed-card',
  imports: [RouterLink, TranslatePipe, RelativeTimePipe, HasPermissionDirective, Icon],
  templateUrl: './activity-feed-card.html',
  styleUrl: './activity-feed-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActivityFeedCard {
  protected readonly icons = { ArrowRight };
  readonly activity = input.required<readonly ActivityRDto[]>();
  readonly limit = input<number>();

  protected readonly items = computed(() => {
    const limit = this.limit();
    const items = limit ? this.activity().slice(0, limit) : this.activity();
    return items.map((item) => ({
      ...item,
      icon: ICONS[item.kind],
      params: {
        ...item.params,
        amount:
          typeof item.params['amount'] === 'number' ? formatAriary(item.params['amount']) : '',
      } as Readonly<Record<string, string | number>>,
    }));
  });
}
