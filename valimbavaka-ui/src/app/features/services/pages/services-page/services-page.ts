import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { TabItem, Tabs } from '../../../../shared/components/tabs/tabs';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { errorMessage } from '../../../../core/http/problem-detail';
import { SalonForm } from '../../components/salon-form/salon-form';
import { ShootingList } from '../../components/shooting-list/shooting-list';
import { ShootingRDto } from '../../data-access/service-models';
import { ServiceRecordWDto } from '../../data-access/service-record-models';
import { ServiceRecordService } from '../../data-access/service-record-service';
import { SalonService } from '../../data-access/service-service';

type ServicesTab = 'salon' | 'shooting';

/**
 * Salon et shooting : enregistrement des prestations et prestations du jour (SRV-02, API), sorties de produits
 * shooting (SHO-01 à SHO-03, démonstration).
 */
@Component({
  selector: 'vvs-services-page',
  imports: [TranslatePipe, AriaryPipe, ListState, Tabs, SalonForm, ShootingList],
  templateUrl: './services-page.html',
  styleUrl: './services-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ServicesPage {
  private readonly salonService = inject(SalonService);
  private readonly serviceRecords = inject(ServiceRecordService);
  private readonly toast = inject(ToastService);

  protected readonly context = rxResource({ stream: () => this.salonService.context() });
  protected readonly tab = signal<ServicesTab>('salon');
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);
  protected readonly returnedNumbers = signal<readonly string[]>([]);

  protected readonly shootings = computed<readonly ShootingRDto[]>(() =>
    (this.context.value()?.shootings ?? []).map((shooting) =>
      this.returnedNumbers().includes(shooting.number)
        ? { ...shooting, status: 'RETURNED' }
        : shooting,
    ),
  );
  protected readonly active = computed(() =>
    this.shootings().filter((item) => item.status !== 'RETURNED'),
  );
  protected readonly tabs = computed<readonly TabItem<ServicesTab>[]>(() => [
    { id: 'salon', labelKey: 'services.tab.salon' },
    { id: 'shooting', labelKey: 'services.tab.shooting', count: this.active().length },
  ]);
  protected readonly today = computed(() => this.context.value()?.today ?? []);
  protected readonly todayTotal = computed(() =>
    this.today().reduce((sum, service) => sum + service.price, 0),
  );

  /** Le serveur déduit le stock salon ; la page relit ensuite les prestations du jour et le stock. */
  protected register(request: ServiceRecordWDto): void {
    const name =
      this.context.value()?.treatments.find((item) => item.id === request.salonServiceId)?.name ??
      '';
    this.saving.set(true);
    this.saveError.set(null);
    this.serviceRecords.create(request).subscribe({
      next: (result) => {
        this.saving.set(false);
        if (result.queued) {
          this.toast.show('common.queuedSaved', {}, 'info');
          return;
        }
        this.toast.show('services.registeredToast', { name, number: result.body?.number ?? '' });
        this.context.reload();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.saveError.set(errorMessage(error));
      },
    });
  }

  protected returnShooting(number: string): void {
    this.salonService.returnShooting(number).subscribe(() => {
      this.returnedNumbers.update((all) => [...all, number]);
      this.toast.show('services.shooting.returnedToast', { number });
    });
  }
}
