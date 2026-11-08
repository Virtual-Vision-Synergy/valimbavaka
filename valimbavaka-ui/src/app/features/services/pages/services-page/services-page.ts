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
import { ShootingKpis } from '../../components/shooting-kpis/shooting-kpis';
import { ShootingList } from '../../components/shooting-list/shooting-list';
import { ShootingRowRDto, ShootingWDto } from '../../data-access/service-models';
import { ServiceRecordWDto } from '../../data-access/service-record-models';
import { ServiceRecordService } from '../../data-access/service-record-service';
import { SalonService } from '../../data-access/service-service';
import { ShootingService } from '../../data-access/shooting-service';

type ServicesTab = 'salon' | 'shooting';

/**
 * Salon et shooting : enregistrement des prestations (SRV-02, API), historique de mes prestations du jour avec heure,
 * service, cliente et produits consommés (SRV-03, API), sorties de produits shooting et leur suivi (SHO-01 à SHO-03,
 * API).
 */
@Component({
  selector: 'vvs-services-page',
  imports: [
    TranslatePipe,
    AriaryPipe,
    ListState,
    Tabs,
    SalonForm,
    ShootingForm,
    ShootingKpis,
    ShootingList,
  ],
  templateUrl: './services-page.html',
  styleUrl: './services-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ServicesPage {
  private readonly salonService = inject(SalonService);
  private readonly serviceRecords = inject(ServiceRecordService);
  private readonly shootingService = inject(ShootingService);
  private readonly toast = inject(ToastService);
  private readonly shootingForm = viewChild(ShootingForm);

  protected readonly context = rxResource({ stream: () => this.salonService.context() });
  /** Sorties shooting réelles (SHO-01) ; le salon lit l'API (SRV-02). */
  protected readonly shootingRows = rxResource({ stream: () => this.shootingService.list() });
  private readonly authStore = inject(AuthStore);
  protected readonly canRecordShooting = computed(() =>
    this.authStore.hasPermission('shootings.create'),
  );
  protected readonly canReturnShooting = computed(() =>
    this.authStore.hasPermission('shootings.return'),
  );
  protected readonly canTrackShooting = computed(() =>
    this.authStore.hasPermission('shootings.track'),
  );
  protected readonly tab = signal<ServicesTab>('salon');
  /** Articles et stock par emplacement, chargés à l'ouverture de l'onglet Shooting si la saisie est permise. */
  protected readonly shootingStock = rxResource({
    params: () => this.canRecordShooting() && this.tab() === 'shooting',
    stream: ({ params }) => (params ? this.shootingService.stock() : of(null)),
  });
  /** Indicateurs des sorties en cours (SHO-03), chargés à l'ouverture de l'onglet Shooting avec `shootings.track`. */
  protected readonly shootingSummary = rxResource({
    params: () => this.canTrackShooting() && this.tab() === 'shooting',
    stream: ({ params }) => (params ? this.shootingService.summary() : of(null)),
  });
  protected readonly savingShooting = signal(false);
  protected readonly shootingError = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);
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
  protected readonly today = computed(() => this.context.value()?.today ?? []);
  protected readonly todayTotal = computed(() =>
    this.today().reduce((sum, service) => sum + service.price, 0),
  );

  /** Le serveur déduit le stock salon ; la page relit ensuite mes prestations du jour et le stock. */
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
        this.reloadShootings();
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
        this.reloadShootings();
      },
      error: (error: unknown) =>
        this.toast.show(
          'services.shooting.returnFailed',
          { number, message: errorMessage(error) },
          'danger',
        ),
    });
  }

  /** Après une écriture acceptée : liste, stock du formulaire et indicateurs relus depuis le serveur. */
  private reloadShootings(): void {
    this.shootingRows.reload();
    this.shootingStock.reload();
    this.shootingSummary.reload();
  }
}
