import { ComponentFixture, TestBed } from '@angular/core/testing';
import axe from 'axe-core';
import { Observable, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ApiError } from '../../../../core/http/problem-detail';
import { SubmitResult } from '../../../../core/offline/outbox-models';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { toDriverRound } from '../../data-access/delivery-mapping';
import {
  DeliveryOrderRDto,
  DeliveryOrderStatus,
  DriverRoundRDto,
} from '../../data-access/delivery-models';
import { DeliveryService } from '../../data-access/delivery-service';
import { DriverHistoryPage } from './driver-history-page';

const DRIVER = { id: 'tojo', firstName: 'Tojo', lastName: 'Nirina' };
const MINUTE = 60_000;

function order(
  stopNumber: number,
  customerName: string,
  status: DeliveryOrderStatus,
  changedMinutesAgo: number,
): DeliveryOrderRDto {
  return {
    id: `order-${stopNumber}`,
    number: `LIV-000${stopNumber}`,
    status,
    customerName,
    customerPhone: '034 12 345 67',
    address: 'Lot II J 45, rue Pasteur',
    district: 'Ivandry',
    driverId: DRIVER.id,
    scheduledDate: '2026-11-11',
    stopNumber,
    amount: 8500,
    note: null,
    failureReason: status === 'FAILED' ? 'ABSENT' : null,
    failureNote: null,
    returnTransferId: null,
    lines: [{ articleId: 'soap', quantity: 1, unitPrice: 8500, amount: 8500 }],
    createdAt: '2026-11-11T05:32:00Z',
    createdBy: 'seller',
    saleId: null,
    previousStatus: status === 'DELIVERED' || status === 'FAILED' ? 'TO_PREPARE' : null,
    updatedAt: new Date(Date.now() - changedMinutesAgo * MINUTE).toISOString(),
    version: 0,
  };
}

describe('DriverHistoryPage', () => {
  let fixture: ComponentFixture<DriverHistoryPage>;
  let element: HTMLElement;
  const myRound = vi.fn<() => Observable<DriverRoundRDto>>();
  const undoLastAction = vi.fn<(id: string) => Observable<SubmitResult<DeliveryOrderRDto>>>();

  const open = async (orders: DeliveryOrderRDto[]): Promise<void> => {
    myRound.mockReset().mockReturnValue(of(toDriverRound(orders, DRIVER, '2026-11-11')));
    undoLastAction.mockReset().mockReturnValue(of({ queued: true }));
    await TestBed.configureTestingModule({
      imports: [DriverHistoryPage],
      providers: [
        provideTestTranslations(),
        { provide: DeliveryService, useValue: { myRound, undoLastAction } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(DriverHistoryPage);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const undoButtons = (): HTMLButtonElement[] =>
    Array.from(element.querySelectorAll<HTMLButtonElement>('button.undo'));

  it('propose d’annuler une livraison ou un échec de moins de 5 minutes, pas les plus anciens', async () => {
    await open([
      order(1, 'Rakoto Be', 'DELIVERED', 2),
      order(2, 'Rasoa Vola', 'FAILED', 1),
      order(3, 'Hery Andry', 'DELIVERED', 30),
    ]);

    const labels = undoButtons().map((button) => button.getAttribute('aria-label'));

    expect(labels).toEqual(
      expect.arrayContaining(["Annuler l'action sur Rakoto Be", "Annuler l'action sur Rasoa Vola"]),
    );
    expect(labels).toHaveLength(2);
  });

  it('annule la dernière action, annonce le résultat et relit la tournée', async () => {
    await open([order(1, 'Rakoto Be', 'DELIVERED', 1)]);
    const show = vi.spyOn(TestBed.inject(ToastService), 'show');

    undoButtons()[0].click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(undoLastAction).toHaveBeenCalledWith('order-1');
    expect(show).toHaveBeenCalledWith('deliveries.driver.undoneToast', { name: 'Rakoto Be' });
    expect(myRound).toHaveBeenCalledTimes(2);
  });

  it('annonce une annulation refusée par le serveur (délai dépassé) et relit la tournée', async () => {
    await open([order(1, 'Rakoto Be', 'DELIVERED', 1)]);
    const message = 'Le délai d’annulation de 5 minute(s) est dépassé.';
    undoLastAction.mockReturnValue(throwError(() => new ApiError(409, 'CONFLICT', message)));
    const show = vi.spyOn(TestBed.inject(ToastService), 'show');

    undoButtons()[0].click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(show).toHaveBeenCalledWith(
      'deliveries.driver.undoRefusedToast',
      { name: 'Rakoto Be', message },
      'danger',
    );
    expect(myRound).toHaveBeenCalledTimes(2);
  });

  it('n’a aucune violation d’accessibilité détectable', async () => {
    await open([order(1, 'Rakoto Be', 'DELIVERED', 1)]);
    document.body.appendChild(element);

    const results = await axe.run(element, {
      runOnly: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'],
      rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
    });

    expect(results.violations.map((violation) => violation.id)).toEqual([]);
    element.remove();
  });
});
