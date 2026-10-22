import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MoveRight } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { TransferService } from '../../data-access/transfer-service';
import { TRANSFER_STATUS_TONES } from '../../data-access/transfer-tones';
import { WhenPipe } from '../../data-access/when-pipe';

/** Bon de transfert (TRF-04) : trajet, effet sur les stocks, lignes et écarts de réception. */
@Component({
  selector: 'vvs-transfer-detail',
  imports: [TranslatePipe, WhenPipe, Icon, ListState, StatusBadge],
  templateUrl: './transfer-detail.html',
  styleUrl: './transfer-detail.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TransferDetail {
  private readonly transferService = inject(TransferService);

  readonly transferId = input.required<string>();
  readonly print = output<void>();
  readonly createReturn = output<void>();

  protected readonly icons = { MoveRight };
  protected readonly tones = TRANSFER_STATUS_TONES;
  protected readonly transfer = rxResource({
    params: () => this.transferId(),
    stream: ({ params }) => this.transferService.get(params),
  });
}
