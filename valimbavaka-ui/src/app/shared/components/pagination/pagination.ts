import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { ChevronLeft, ChevronRight } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { Icon } from '../icon/icon';

/** Pagination base 1 (convention API : page ≥ 1, size ≤ 100) : « 1–10 sur 142 » + précédent/suivant. */
@Component({
  selector: 'vvs-pagination',
  imports: [TranslatePipe, Icon],
  templateUrl: './pagination.html',
  styleUrl: './pagination.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Pagination {
  protected readonly icons = { ChevronLeft, ChevronRight };
  readonly page = model.required<number>();
  readonly size = input.required<number>();
  readonly total = input.required<number>();

  protected readonly pageCount = computed(() => Math.max(1, Math.ceil(this.total() / this.size())));
  protected readonly from = computed(() =>
    this.total() === 0 ? 0 : (this.page() - 1) * this.size() + 1,
  );
  protected readonly to = computed(() => Math.min(this.total(), this.page() * this.size()));
}
