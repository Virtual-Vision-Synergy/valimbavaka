import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { Check } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../../../../shared/components/icon/icon';
import { ClosingChecklistRDto } from '../../data-access/day-models';

export type ChecklistAction = 'reintegrate' | 'showGaps';

interface CheckItem {
  readonly id: string;
  readonly ok: boolean;
  readonly valueKey: string;
  readonly params: Readonly<Record<string, number>>;
  readonly action: ChecklistAction | null;
}

type CheckValue = Partial<Pick<CheckItem, 'params' | 'action'>> & Pick<CheckItem, 'valueKey'>;

function check(id: string, ok: boolean, value: CheckValue): CheckItem {
  return { id, ok, params: {}, action: null, ...value };
}

/** Conditions du verrouillage (JR-06) : tant qu'un point n'est pas vert, la journée reste ouverte. */
@Component({
  selector: 'vvs-closing-checklist',
  imports: [TranslatePipe, Icon],
  templateUrl: './closing-checklist.html',
  styleUrl: './closing-checklist.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClosingChecklist {
  protected readonly icons = { Check };
  readonly checklist = input.required<ClosingChecklistRDto>();
  readonly lateShootings = input.required<number>();
  readonly missingReasons = input.required<number>();
  readonly locked = input(false);
  readonly action = output<ChecklistAction>();

  protected readonly items = computed<readonly CheckItem[]>(() => {
    const { deliveriesDone, deliveriesTotal, heldTickets } = this.checklist();
    const late = this.lateShootings();
    const missing = this.missingReasons();
    const done = 'day.close.check.allDone';
    return [
      check('deliveries', deliveriesDone === deliveriesTotal, {
        valueKey: 'day.close.check.deliveriesValue',
        params: { done: deliveriesDone, total: deliveriesTotal },
      }),
      check('tickets', heldTickets === 0, {
        valueKey: 'day.close.check.ticketsValue',
        params: { count: heldTickets },
      }),
      late
        ? check('shootings', false, {
            valueKey: 'day.close.check.shootingsLate',
            params: { count: late },
            action: 'reintegrate',
          })
        : check('shootings', true, { valueKey: done }),
      missing
        ? check('reasons', false, {
            valueKey: 'day.close.check.reasonsMissing',
            params: { count: missing },
            action: 'showGaps',
          })
        : check('reasons', true, { valueKey: done }),
    ];
  });
}
