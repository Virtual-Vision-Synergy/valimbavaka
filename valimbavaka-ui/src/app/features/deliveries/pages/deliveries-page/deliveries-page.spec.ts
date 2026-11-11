import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import axe from 'axe-core';
import { Observable, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { toBoard } from '../../data-access/delivery-mapping';
import {
  DeliveryBoardRDto,
  DeliveryEventRDto,
  DeliveryOrderRDto,
  DeliveryOrderStatus,
} from '../../data-access/delivery-models';
import { DeliveryService } from '../../data-access/delivery-service';
import { DeliveriesPage } from './deliveries-page';

const TOJO = { id: 'tojo', firstName: 'Tojo', lastName: 'Nirina' };

function order(stopNumber: number, status: DeliveryOrderStatus): DeliveryOrderRDto {
  const number = `LIV-000${stopNumber}`;
  return {
    id: `id-${number}`,
    number,
    status,
    customerName: `Client ${stopNumber}`,
    customerPhone: '034 12 345 67',
    address: 'Lot II J 45, rue Pasteur',
    district: 'Ivandry',
    driverId: TOJO.id,
    scheduledDate: '2026-11-08',
    stopNumber,
    amount: 8500,
    note: null,
    failureReason: null,
    failureNote: null,
    returnTransferId: null,
    lines: [{ articleId: 'soap', quantity: 1, unitPrice: 8500, amount: 8500 }],
    createdAt: '2026-11-08T05:32:00Z',
    createdBy: 'seller',
    version: 0,
  };
}

const BOARD = toBoard(
  [order(1, 'TO_PREPARE'), order(2, 'DELIVERED'), order(3, 'CANCELLED'), order(4, 'FAILED')],
  [TOJO],
);

const HISTORY: DeliveryEventRDto[] = [
  {
    occurredAt: '2026-11-08T06:15:00Z',
    actorName: 'Hanta Rabe',
    action: 'CREATE',
    description: 'Création de la commande à livrer LIV-0001 : 1 article(s), 8500.00 Ar',
  },
];

describe('DeliveriesPage', () => {
  let fixture: ComponentFixture<DeliveriesPage>;
  let element: HTMLElement;
  const board = vi.fn<(day?: string) => Observable<DeliveryBoardRDto>>();
  const history = vi.fn<(orderId: string) => Observable<DeliveryEventRDto[]>>();

  const open = async (): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [DeliveriesPage],
      providers: [
        provideRouter([]),
        provideTestTranslations(),
        { provide: DeliveryService, useValue: { board, history } },
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
    fixture = TestBed.createComponent(DeliveriesPage);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const columnTitles = (): string[] =>
    [...element.querySelectorAll('.column-title')].map(
      (title) => title.textContent?.replace(/\s+/g, ' ').trim() ?? '',
    );
  const cardNumbers = (): string[] =>
    [...element.querySelectorAll('vvs-delivery-card .number')].map(
      (number) => number.textContent?.trim() ?? '',
    );
  const statusChip = (label: string): HTMLButtonElement => {
    const group = element.querySelectorAll('[role="group"]')[1];
    const chip = [...group.querySelectorAll('button')].find((button) =>
      button.textContent?.includes(label),
    );
    if (!chip) {
      throw new Error(`Pastille introuvable : ${label}`);
    }
    return chip;
  };
  const select = async (chip: HTMLButtonElement): Promise<void> => {
    chip.click();
    fixture.detectChanges();
    await fixture.whenStable();
  };

  beforeEach(() => {
    board.mockReset().mockReturnValue(of(BOARD));
    history.mockReset().mockReturnValue(of(HISTORY));
  });

  it('montre les quatre colonnes du jour sans les commandes annulées par défaut', async () => {
    await open();

    expect(columnTitles()).toEqual(['À préparer 1', 'En tournée 0', 'Livrées 1', 'Échecs 1']);
    expect(cardNumbers()).toEqual(['LIV-0001', 'LIV-0002', 'LIV-0004']);
    expect(statusChip('Tous les statuts').textContent).toContain('3');
    expect(statusChip('Annulée').textContent).toContain('1');
  });

  it('filtre par statut : « Annulée » ne montre que les commandes annulées', async () => {
    await open();

    await select(statusChip('Annulée'));

    expect(columnTitles()).toEqual(['Annulées 1']);
    expect(cardNumbers()).toEqual(['LIV-0003']);
    expect(statusChip('Annulée').getAttribute('aria-pressed')).toBe('true');
  });

  it('filtre par statut : « Échec » (retour) ne montre que les échecs, puis « Tous » rend le tableau', async () => {
    await open();

    await select(statusChip('Échec'));
    expect(columnTitles()).toEqual(['Échecs 1']);
    expect(cardNumbers()).toEqual(['LIV-0004']);

    await select(statusChip('Tous les statuts'));
    expect(columnTitles()).toHaveLength(4);
  });

  it('relit le tableau du jour choisi pour consulter l’historique d’un jour passé', async () => {
    await open();
    const input = element.querySelector<HTMLInputElement>('input[type="date"]');
    if (!input) {
      throw new Error('Champ jour absent');
    }

    input.value = '2026-11-01';
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(board).toHaveBeenLastCalledWith('2026-11-01');
  });

  it('ouvre le détail avec l’historique horodaté et attribué de la commande', async () => {
    await open();

    element.querySelector<HTMLButtonElement>('vvs-delivery-card button')?.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(history).toHaveBeenCalledWith('id-LIV-0001');
    const step = element.querySelector('.timeline li')?.textContent?.replace(/\s+/g, ' ') ?? '';
    expect(step).toContain('Commande créée');
    expect(step).toContain('08/11 09:15 · Hanta Rabe');
    expect(step).toContain('Création de la commande à livrer LIV-0001');
    const results = await axe.run(element.querySelector('vvs-delivery-detail') as HTMLElement, {
      rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
    });
    expect(results.violations.map((violation) => violation.id)).toEqual([]);
  });

  it('propose de réessayer quand l’historique ne se charge pas', async () => {
    history.mockReturnValue(throwError(() => new Error('hors ligne')));
    await open();

    element.querySelector<HTMLButtonElement>('vvs-delivery-card button')?.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(element.querySelector('vvs-delivery-detail vvs-list-state')).not.toBeNull();
    expect(element.querySelector('.timeline')).toBeNull();
  });
});
