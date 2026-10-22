import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Download } from 'lucide';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language-service';
import { Icon } from '../../../../shared/components/icon/icon';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { StatusBadge } from '../../../../shared/components/status-badge/status-badge';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { ShortDatePipe } from '../../../../shared/pipes/short-date-pipe';
import { Tone } from '../../../../shared/models/tone';
import { COMMISSION_MONTHS } from '../../data-access/commission-fixtures';
import { CommissionStatus } from '../../data-access/commission-models';
import { CommissionService } from '../../data-access/commission-service';

/** Circonférence du cercle de progression (rayon 52). */
const RING_CIRCUMFERENCE = 2 * Math.PI * 52;

const STATUS_TONES: Readonly<Record<CommissionStatus, Tone>> = {
  CALCULATED: 'warning',
  VALIDATED: 'success',
  PAID: 'info',
};

/** Mes commissions (COM-01 à COM-06) : estimée en temps réel, validée par la direction, versée le mois suivant. */
@Component({
  selector: 'vvs-commissions-page',
  imports: [TranslatePipe, AriaryPipe, ShortDatePipe, Icon, ListState, PageHeader, StatusBadge],
  templateUrl: './commissions-page.html',
  styleUrl: './commissions-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CommissionsPage {
  private readonly commissionService = inject(CommissionService);
  private readonly toast = inject(ToastService);
  private readonly language = inject(LanguageService);

  protected readonly icons = { Download };
  protected readonly tones = STATUS_TONES;
  protected readonly circumference = RING_CIRCUMFERENCE;
  protected readonly month = signal(COMMISSION_MONTHS[0]);
  protected readonly statement = rxResource({
    params: () => this.month(),
    stream: ({ params }) => this.commissionService.statement(params),
  });

  protected readonly months = computed(() =>
    COMMISSION_MONTHS.map((value) => ({ value, label: this.monthLabel(value) })),
  );
  protected readonly progress = computed(() => {
    const data = this.statement.value();
    return data ? Math.min(100, Math.round((data.sales / data.goal) * 100)) : 0;
  });
  protected readonly remaining = computed(() => {
    const data = this.statement.value();
    if (!data || data.workingDaysLeft === 0) {
      return null;
    }
    const left = Math.max(0, data.goal - data.sales);
    return {
      left,
      perDay: Math.round(left / data.workingDaysLeft / 1000) * 1000,
      days: data.workingDaysLeft,
    };
  });
  protected readonly estimate = computed(() => {
    const data = this.statement.value();
    return data ? data.validated + data.toValidate : 0;
  });

  protected selectMonth(value: string): void {
    this.month.set(value);
  }

  protected exportStatement(): void {
    this.toast.show('common.demoAction', {}, 'info');
  }

  protected monthLabel(value: string): string {
    const label = new Intl.DateTimeFormat(this.language.current(), {
      month: 'long',
      year: 'numeric',
    }).format(new Date(`${value}-01T12:00:00`));
    return label.charAt(0).toUpperCase() + label.slice(1);
  }
}
