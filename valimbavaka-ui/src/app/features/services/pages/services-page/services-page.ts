import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { of } from 'rxjs';
import { AuthStore } from '../../../../core/auth/auth-store';
import { errorMessage } from '../../../../core/http/problem-detail';
import { ListState } from '../../../../shared/components/list-state/list-state';
import { TabItem, Tabs } from '../../../../shared/components/tabs/tabs';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { AriaryPipe } from '../../../../shared/pipes/ariary-pipe';
import { SalonForm } from '../../components/salon-form/salon-form';
import { ShootingForm } from '../../components/shooting-form/shooting-form';
import { ShootingList } from '../../components/shooting-list/shooting-list';
import {
  SalonServiceRDto,
  SalonServiceWDto,
  ShootingRowRDto,
  ShootingWDto,
} from '../../data-access/service-models';
import { SalonService } from '../../data-access/service-service';
import { ShootingService } from '../../data-access/shooting-service';
import { isoDay } from '../../../../shared/data-access/demo-dates';

type ServicesTab = 'salon' | 'shooting';

/** Salon et shooting (SRV-01 à SRV-04, SHO-01 à SHO-03) : prestations du jour et sorties de produits. */
@Component({
  selector: 'vvs-services-page',
  imports: [TranslatePipe, AriaryPipe, ListState, Tabs, SalonForm, ShootingForm, ShootingList],
  templateUrl: './services-page.html',
  styleUrl: './services-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ServicesPage {
  private readonly salonService = inject(SalonService);
  private readonly shootingService = inject(ShootingService);
  private readonly toast = inject(ToastService);
  private readonly shootingForm = viewChild(ShootingForm);

  protected readonly context = rxResource({ stream: () => this.salonService.context() });
  /** Sorties shooting réelles (SHO-01) ; le salon reste en démonstration (SRV-xx). */
  protected readonly shootingRows = rxResource({ stream: () => this.shootingService.list() });
  private readonly authStore = inject(AuthStore);
  protected readonly canRecordShooting = computed(() =>
    this.authStore.hasPermission('shootings.create'),
  );
  protected readonly canReturnShooting = computed(() =>
    this.authStore.hasPermission('shootings.return'),
  );
  protected readonly tab = signal<ServicesTab>('salon');
  /** Articles et stock par emplacement, chargés à l'ouverture de l'onglet Shooting si la saisie est permise. */
  protected readonly shootingStock = rxResource({
    params: () => this.canRecordShooting() && this.tab() === 'shooting',
    stream: ({ params }) => (params ? this.shootingService.stock() : of(null)),
  });
  protected readonly savingShooting = signal(false);
  protected readonly shootingError = signal<string | null>(null);
  protected readonly created = signal<readonly SalonServiceRDto[]>([]);
  /** Réintégrations en file hors ligne : affichées « Réintégré » en attendant le serveur. */
  protected readonly returnedNumbers = signal<readonly string[]>([]);

  protected readonly shootings = computed<readonly ShootingRowRDto[]>(() =>
    (this.shootingRows.value() ?? []).map((shooting) =>
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

  protected recordShooting(request: ShootingWDto): void {
    this.savingShooting.set(true);
    this.shootingError.set(null);
    this.shootingService.create(request).subscribe({
      next: (result) => {
        this.savingShooting.set(false);
        this.shootingForm()?.reset();
        if (result.queued) {
          this.toast.show('common.queuedSaved', {}, 'info');
          return;
        }
        this.toast.show('services.shooting.form.savedToast', { number: result.body?.number ?? '' });
        this.shootingRows.reload();
        this.shootingStock.reload();
      },
      error: (error: unknown) => {
        this.savingShooting.set(false);
        this.shootingError.set(errorMessage(error));
      },
    });
  }

  /** Réintégration (SHO-02) : statut Réintégré et stock de l'emplacement de sortie remis à jour par le serveur. */
  protected returnShooting(number: string): void {
    const shooting = this.shootings().find((row) => row.number === number);
    if (!shooting) {
      return;
    }
    this.shootingService.returnShooting(shooting.id).subscribe({
      next: (result) => {
        if (result.queued) {
          this.returnedNumbers.update((all) => [...all, number]);
          this.toast.show('common.queuedSaved', {}, 'info');
          return;
        }
        this.toast.show('services.shooting.returnedToast', { number });
        this.shootingRows.reload();
        this.shootingStock.reload();
      },
      error: (error: unknown) =>
        this.toast.show(
          'services.shooting.returnFailed',
          { number, message: errorMessage(error) },
          'danger',
        ),
    });
  }
}
