import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MapPin } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../../../../shared/components/icon/icon';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { DeliveryRDto } from '../../data-access/delivery-models';

/** Carte d'une commande dans sa colonne de statut (LIV-01) ; sélectionnable pour le détail. */
@Component({
  selector: 'vvs-delivery-card',
  imports: [TranslatePipe, AriaryPipe, Icon],
  templateUrl: './delivery-card.html',
  styleUrl: './delivery-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeliveryCard {
  protected readonly icons = { MapPin };
  readonly delivery = input.required<DeliveryRDto>();
  readonly stopCount = input<number>(0);
  readonly selected = input(false);
  readonly picked = output<string>();

  /** Clé + paramètres de la mention en bas de carte, selon le statut. */
  protected readonly footer = computed(() => {
    const delivery = this.delivery();
    switch (delivery.status) {
      case 'TO_PREPARE':
        return { key: `sales.channel.${delivery.channel}`, params: {}, tone: 'muted' };
      case 'ON_ROUND':
        return {
          key: 'deliveries.card.stop',
          params: { stop: delivery.stop ?? 0, count: this.stopCount() },
          tone: 'info',
        };
      case 'DELIVERED':
        return {
          key: delivery.prepaid ? 'deliveries.card.prepaid' : 'deliveries.card.collected',
          params: {},
          tone: 'success',
        };
      case 'FAILED':
        return {
          key: `deliveries.reason.${delivery.failureReason ?? 'OTHER'}`,
          params: {},
          tone: 'danger',
        };
    }
  });
}
