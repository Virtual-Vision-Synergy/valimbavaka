import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { WhenPipe } from '../../../transfers/data-access/when-pipe';
import { ShootingRDto, ShootingStatus } from '../../data-access/service-models';
import { Tone } from '../../../../shared/models/tone';

const TONES: Readonly<Record<ShootingStatus, Tone>> = {
  OUT: 'warning',
  LATE: 'danger',
  RETURNED: 'success',
};

/** Sorties de produits pour shooting (SHO-01 à SHO-03) : retour prévu, retards, réintégration. */
@Component({
  selector: 'vvs-shooting-list',
  imports: [TranslatePipe, WhenPipe, StatusBadge],
  templateUrl: './shooting-list.html',
  styleUrl: './shooting-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShootingList {
  readonly shootings = input.required<readonly ShootingRDto[]>();
  readonly returned = output<string>();
  protected readonly tones = TONES;
}
