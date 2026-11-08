import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import axe from 'axe-core';
import { Observable, of } from 'rxjs';
import { vi } from 'vitest';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { OutboxService } from '../../../../core/offline/outbox-service';
import { formatAriary } from '../../../../shared/pipes/ariary-pipe';
import { toDriverRound } from '../../data-access/delivery-mapping';
import {
  DeliveryOrderRDto,
  DeliveryOrderStatus,
  DriverRoundRDto,
} from '../../data-access/delivery-models';
import { DeliveryService } from '../../data-access/delivery-service';
import { DriverRoundPage } from './driver-round-page';

const DRIVER = { id: 'tojo', firstName: 'Tojo', lastName: 'Nirina' };

function order(
  stopNumber: number,
  customerName: string,
  status: DeliveryOrderStatus = 'TO_PREPARE',
): DeliveryOrderRDto {
  return {
    id: `order-${stopNumber}`,
    number: `LIV-000${stopNumber}`,
    status,
    customerName,
    customerPhone: `034 12 345 6${stopNumber}`,
    address: `Lot II J ${stopNumber}, rue Pasteur`,
    district: 'Ivandry',
    driverId: DRIVER.id,
    scheduledDate: '2026-11-09',
    stopNumber,
    amount: 8500 * stopNumber,
    note: null,
    lines: [
      { articleId: 'soap', quantity: stopNumber, unitPrice: 8500, amount: 8500 * stopNumber },
    ],
    createdAt: '2026-11-09T05:32:00Z',
    createdBy: 'seller',
    version: 0,
  };
}

const ROUND = toDriverRound(
  [order(1, 'Rakoto Be', 'DELIVERED'), order(2, 'Rasoa Vola'), order(3, 'Hery Andry', 'ON_ROUND')],
  DRIVER,
  '2026-11-09',
);

describe('DriverRoundPage', () => {
  let fixture: ComponentFixture<DriverRoundPage>;
  let element: HTMLElement;
  const myRound = vi.fn<() => Observable<DriverRoundRDto>>();

  const open = async (round: DriverRoundRDto): Promise<void> => {
    myRound.mockReset().mockReturnValue(of(round));
    await TestBed.configureTestingModule({
      imports: [DriverRoundPage],
      providers: [
        provideTestTranslations(),
        { provide: DeliveryService, useValue: { myRound, updateStatus: vi.fn() } },
        { provide: OutboxService, useValue: { pendingCount: signal(0) } },
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('DELIVERY_DRIVER'));
    fixture = TestBed.createComponent(DriverRoundPage);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const text = (selector: string): string =>
    element.querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim() ?? '';

  it('met en avant le premier arrêt à faire avec client, adresse, téléphone et montant à encaisser', async () => {
    await open(ROUND);

    expect(myRound).toHaveBeenCalledOnce();
    expect(text('.next h2')).toBe('Rasoa Vola');
    expect(text('.next .address')).toBe('Ivandry, Lot II J 2, rue Pasteur');
    expect(text('.next .phone')).toBe('034 12 345 62');
    expect(element.querySelector('.next .contact a')?.getAttribute('href')).toBe('tel:0341234562');
    expect(element.querySelector('.next .amount strong')?.textContent?.trim()).toBe(
      formatAriary(17000),
    );
  });

  it('liste les arrêts suivants dans l’ordre avec leur adresse, leur téléphone et leur montant', async () => {
    await open(ROUND);

    const later = [...element.querySelectorAll('.later li')];
    expect(later).toHaveLength(1);
    expect(later[0].textContent).toContain('Hery Andry');
    expect(later[0].textContent).toContain('Ivandry, Lot II J 3, rue Pasteur');
    expect(later[0].querySelector('a.tel')?.getAttribute('href')).toBe('tel:0341234563');
    expect(later[0].textContent).toContain(formatAriary(25500));
    expect(text('.tiles .tile:first-child dd')).toBe('1/3');
  });

  it('annonce une journée sans arrêt plutôt qu’une tournée terminée', async () => {
    await open({ ...ROUND, stops: [] });

    expect(text('.empty h2')).toBe("Aucun arrêt aujourd'hui");
    expect(element.querySelector('.next')).toBeNull();
  });

  it('n’a aucune violation d’accessibilité détectable', async () => {
    await open(ROUND);
    document.body.appendChild(element);

    const results = await axe.run(element, {
      runOnly: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'],
      rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
    });

    expect(results.violations.map((violation) => violation.id)).toEqual([]);
    element.remove();
  });
});
