import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { SubmitResult } from '../../../../core/offline/outbox-models';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { PRODUCTION_FIXTURE } from '../../data-access/production-fixtures';
import { LotRDto, ProductionBatchRDto } from '../../data-access/production-models';
import { ProductionService } from '../../data-access/production-service';
import { ProductionPage } from './production-page';

type Answer = Observable<SubmitResult<ProductionBatchRDto>>;

const lot = (id: string, status: LotRDto['status']): LotRDto => ({
  id,
  number: id.toUpperCase(),
  date: '2026-11-07',
  articleName: 'Savon noir',
  quantity: 30,
  operator: 'Soa Rakoto',
  status,
});

const RECENT: readonly LotRDto[] = [
  lot('lot-00001', 'DRAFT'),
  lot('lot-00002', 'CURING'),
  lot('lot-00003', 'VALIDATED'),
];

const answer = (status: ProductionBatchRDto['status']): Answer =>
  of({ queued: false, body: { status } as ProductionBatchRDto });

/** PRD-03 : statut visible sur chaque lot récent et modifiable jusqu'à « Terminé ». */
describe('ProductionPage — statut des lots', () => {
  let fixture: ComponentFixture<ProductionPage>;
  const startLot = vi.fn<(id: string) => Answer>();
  const completeLot = vi.fn<(id: string) => Answer>();

  const open = async (role: 'ADMIN' | 'SELLER' | 'DELIVERY_DRIVER'): Promise<HTMLElement> => {
    await TestBed.configureTestingModule({
      imports: [ProductionPage],
      providers: [
        provideRouter([]),
        provideTestTranslations(),
        {
          provide: ProductionService,
          useValue: {
            context: () => of({ ...PRODUCTION_FIXTURE, recentLots: RECENT }),
            startLot,
            completeLot,
          },
        },
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser(role));
    fixture = TestBed.createComponent(ProductionPage);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  };

  const row = (element: HTMLElement, number: string): HTMLElement =>
    Array.from(element.querySelectorAll<HTMLElement>('.lots li')).find((item) =>
      item.textContent?.includes(number),
    ) as HTMLElement;

  const button = (element: HTMLElement, label: string): HTMLButtonElement | undefined =>
    Array.from(element.querySelectorAll<HTMLButtonElement>('button')).find(
      (item) => item.textContent?.trim() === label,
    );

  beforeEach(() => {
    startLot.mockReset();
    completeLot.mockReset();
  });

  it('affiche le statut de chaque lot : Planifié, En cours, Terminé', async () => {
    const element = await open('SELLER');

    expect(row(element, 'LOT-00001').textContent).toContain('Planifié');
    expect(row(element, 'LOT-00002').textContent).toContain('En cours');
    expect(row(element, 'LOT-00003').textContent).toContain('Terminé');
  });

  it('propose Démarrer et Terminer sur un lot planifié, Terminer seul sur un lot en cours, rien sur un lot terminé', async () => {
    const element = await open('SELLER');

    expect(button(row(element, 'LOT-00001'), 'Démarrer')?.getAttribute('aria-label')).toBe(
      'Démarrer le lot LOT-00001',
    );
    expect(button(row(element, 'LOT-00001'), 'Terminer')).toBeDefined();
    expect(button(row(element, 'LOT-00002'), 'Démarrer')).toBeUndefined();
    expect(button(row(element, 'LOT-00002'), 'Terminer')).toBeDefined();
    expect(row(element, 'LOT-00003').querySelector('button')).toBeNull();
  });

  it('Démarrer passe le lot en cours sans recharger la liste', async () => {
    startLot.mockReturnValue(answer('IN_PROGRESS'));
    const element = await open('SELLER');

    button(row(element, 'LOT-00001'), 'Démarrer')?.click();
    await fixture.whenStable();

    expect(startLot).toHaveBeenCalledWith('lot-00001');
    expect(row(element, 'LOT-00001').textContent).toContain('En cours');
    expect(button(row(element, 'LOT-00001'), 'Démarrer')).toBeUndefined();
    expect(TestBed.inject(ToastService).toasts().at(-1)?.messageKey).toBe(
      'production.startedToast',
    );
  });

  it('Terminer un lot en cours le passe à Terminé et retire ses boutons', async () => {
    completeLot.mockReturnValue(answer('COMPLETED'));
    const element = await open('SELLER');

    button(row(element, 'LOT-00002'), 'Terminer')?.click();
    await fixture.whenStable();

    expect(completeLot).toHaveBeenCalledWith('lot-00002');
    expect(row(element, 'LOT-00002').textContent).toContain('Terminé');
    expect(row(element, 'LOT-00002').querySelector('button')).toBeNull();
  });

  it('garde le statut et affiche l’erreur quand le serveur refuse (stock insuffisant)', async () => {
    completeLot.mockReturnValue(throwError(() => new Error('Stock insuffisant')));
    const element = await open('ADMIN');

    button(row(element, 'LOT-00001'), 'Terminer')?.click();
    await fixture.whenStable();

    expect(row(element, 'LOT-00001').textContent).toContain('Planifié');
    expect(TestBed.inject(ToastService).toasts().at(-1)).toMatchObject({
      messageKey: 'production.statusError',
      tone: 'danger',
    });
  });

  it('hors ligne : la saisie part en file et le statut affiché ne change pas', async () => {
    startLot.mockReturnValue(of({ queued: true }));
    const element = await open('SELLER');

    button(row(element, 'LOT-00001'), 'Démarrer')?.click();
    await fixture.whenStable();

    expect(row(element, 'LOT-00001').textContent).toContain('Planifié');
    expect(TestBed.inject(ToastService).toasts().at(-1)?.messageKey).toBe('common.queuedSaved');
  });
});
