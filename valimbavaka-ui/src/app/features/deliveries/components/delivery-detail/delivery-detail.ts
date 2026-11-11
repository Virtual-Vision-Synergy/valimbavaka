import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { eventTime } from '../../data-access/delivery-mapping';
import { DeliveryRDto } from '../../data-access/delivery-models';
import { DeliveryService } from '../../data-access/delivery-service';
import { DELIVERY_EVENT_TONES, DELIVERY_STATUS_TONES } from '../../data-access/delivery-tones';

/**
 * Détail d'une commande (LIV-06) : client, tournée, montant et historique lu sur l'API (une étape par ligne du
 * journal, avec son heure et son auteur). Les actions de statut arrivent avec LIV-03, LIV-04 et LIV-05.
 */
@Component({
  selector: 'vvs-delivery-detail',
  imports: [RouterLink, TranslatePipe, AriaryPipe, StatusBadge, ListState],
  templateUrl: './delivery-detail.html',
  styleUrl: './delivery-detail.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeliveryDetail {
  private readonly deliveryService = inject(DeliveryService);

  readonly delivery = input.required<DeliveryRDto>();

  protected readonly history = rxResource({
    params: () => this.delivery().id,
    stream: ({ params }) => this.deliveryService.history(params),
  });
  protected readonly statusTones = DELIVERY_STATUS_TONES;
  protected readonly eventTones = DELIVERY_EVENT_TONES;
  protected readonly eventTime = eventTime;
}
