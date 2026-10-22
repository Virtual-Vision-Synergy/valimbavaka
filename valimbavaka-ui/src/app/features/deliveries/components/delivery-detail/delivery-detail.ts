import { LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { DeliveryRDto } from '../../data-access/delivery-models';
import { DELIVERY_EVENT_TONES, DELIVERY_STATUS_TONES } from '../../data-access/delivery-tones';

/** Détail d'une commande et historique de statut (LIV-07), actions selon le statut. */
@Component({
  selector: 'vvs-delivery-detail',
  imports: [LowerCasePipe, RouterLink, TranslatePipe, AriaryPipe, StatusBadge],
  templateUrl: './delivery-detail.html',
  styleUrl: './delivery-detail.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeliveryDetail {
  readonly delivery = input.required<DeliveryRDto>();
  readonly action = output<string>();

  protected readonly statusTones = DELIVERY_STATUS_TONES;
  protected readonly eventTones = DELIVERY_EVENT_TONES;
}
