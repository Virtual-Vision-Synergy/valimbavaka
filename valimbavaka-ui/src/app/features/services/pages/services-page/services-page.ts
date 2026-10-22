import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { TabItem, Tabs } from '../../../../shared/components/tabs/tabs';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { SalonForm } from '../../components/salon-form/salon-form';
import { ShootingList } from '../../components/shooting-list/shooting-list';
import { SalonServiceRDto, SalonServiceWDto, ShootingRDto } from '../../data-access/service-models';
import { SalonService } from '../../data-access/service-service';
import { isoDay } from '../../../../shared/data-access/demo-dates';

type ServicesTab = 'salon' | 'shooting';

/** Salon et shooting (SRV-01 à SRV-04, SHO-01 à SHO-03) : prestations du jour et sorties de produits. */
@Component({
  selector: 'vvs-services-page',
  imports: [TranslatePipe, AriaryPipe, ListState, Tabs, SalonForm, ShootingList],
  templateUrl: './services-page.html',
  styleUrl: './services-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ServicesPage {
  private readonly salonService = inject(SalonService);
  private readonly toast = inject(ToastService);

  protected readonly context = rxResource({ stream: () => this.salonService.context() });
  protected readonly tab = signal<ServicesTab>('salon');
  protected readonly created = signal<readonly SalonServiceRDto[]>([]);
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
  protected readonly today = computed(() => [
    ...this.created(),
    ...(this.context.value()?.today ?? []),
  ]);
  protected readonly todayTotal = computed(() =>
    this.today().reduce((sum, service) => sum + service.price, 0),
  );

  protected register(request: SalonServiceWDto): void {
    const treatment = this.context
      .value()
      ?.treatments.find((item) => item.id === request.treatmentId);
    this.salonService.register(request).subscribe(() => {
      const now = new Date();
      this.created.update((all) => [
        {
          id: `new-${now.getTime()}`,
          treatmentName: treatment?.name ?? '',
          time: now.toTimeString().slice(0, 5),
          staff: request.staff.split(' ')[0] + ' ' + (request.staff.split(' ')[1]?.[0] ?? '') + '.',
          price: treatment?.price ?? 0,
        },
        ...all,
      ]);
      this.toast.show('services.registeredToast', { name: treatment?.name ?? '', date: isoDay(0) });
    });
  }

  protected returnShooting(number: string): void {
    this.salonService.returnShooting(number).subscribe(() => {
      this.returnedNumbers.update((all) => [...all, number]);
      this.toast.show('services.shooting.returnedToast', { number });
    });
  }
}
