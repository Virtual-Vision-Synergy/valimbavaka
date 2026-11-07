import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import axe from 'axe-core';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { BusinessDayRDto, DayOpeningGrid, NO_QUANTITIES } from '../../data-access/day-models';
import { DayService } from '../../data-access/day-service';
import { DayOpenPage } from './day-open-page';

const GRID: DayOpeningGrid = {
  date: '2026-11-08',
  previousDay: '2026-11-07',
  rounds: 0,
  destinationIds: { SHOP: 'store-id', DELIVERY: 'delivery-id', SALON: 'salon-id' },
  vouchers: null,
  lines: [
    {
      articleId: 'soap',
      reference: 'ART-0031',
      name: 'Savon noir',
      threshold: 40,
      warehouse: 20,
      carryOver: null,
      proposed: NO_QUANTITIES,
      yesterday: { SHOP: 12, DELIVERY: 5, SALON: 0 },
    },
    {
      articleId: 'oil',
      reference: 'ART-0087',
      name: 'Huile de coco',
      threshold: 10,
      warehouse: 6,
      carryOver: null,
      proposed: NO_QUANTITIES,
      yesterday: NO_QUANTITIES,
    },
  ],
};

const OPENED: BusinessDayRDto = {
  id: 'day-id',
  businessDate: '2026-11-08',
  status: 'OPEN',
  transfers: [
    {
      transferId: 't1',
      transferNumber: 'TRF-0093',
      locationId: 'store-id',
      lineCount: 1,
      quantity: 12,
    },
    {
      transferId: 't2',
      transferNumber: 'TRF-0094',
      locationId: 'delivery-id',
      lineCount: 1,
      quantity: 5,
    },
  ],
  createdAt: '2026-11-08T04:30:00Z',
  createdBy: null,
};

describe('DayOpenPage', () => {
  const opening = vi.fn();
  const openDay = vi.fn();
  let fixture: ComponentFixture<DayOpenPage>;
  let element: HTMLElement;
  let toast: ToastService;

  const render = async (grid: DayOpeningGrid = GRID): Promise<void> => {
    opening.mockReturnValue(of(grid));
    await TestBed.configureTestingModule({
      imports: [DayOpenPage],
      providers: [
        provideRouter([]),
        provideTestTranslations(),
        { provide: DayService, useValue: { opening, open: openDay } },
      ],
    }).compileComponents();
    toast = TestBed.inject(ToastService);
    fixture = TestBed.createComponent(DayOpenPage);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const type = async (id: string, value: string): Promise<void> => {
    const input = element.querySelector<HTMLInputElement>(`#${id}`) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const validateButton = (): HTMLButtonElement =>
    element.querySelector<HTMLButtonElement>('.summary .btn-primary') as HTMLButtonElement;

  const yesterdayButton = (): HTMLButtonElement =>
    element.querySelector<HTMLButtonElement>(
      '.toolbar-actions .btn-secondary',
    ) as HTMLButtonElement;

  const validate = async (): Promise<void> => {
    validateButton().click();
    fixture.detectChanges();
    await fixture.whenStable();
  };

  beforeEach(() => openDay.mockReset());

  it('liste les articles disponibles à l’entrepôt, quantités à zéro', async () => {
    await render();

    expect(element.querySelectorAll('tbody tr')).toHaveLength(2);
    expect(element.textContent).toContain('Savon noir');
    expect(element.querySelector<HTMLInputElement>('#qty-SHOP-0')?.value).toBe('0');
    expect(validateButton().disabled).toBe(true);
  });

  it('ouvre la journée : un bon par destination, numéros affichés', async () => {
    openDay.mockReturnValue(of({ queued: false, body: OPENED }));
    await render();
    const show = vi.spyOn(toast, 'show');
    await type('qty-SHOP-0', '12');
    await type('qty-DELIVERY-0', '5');

    await validate();

    expect(openDay).toHaveBeenCalledWith({
      lines: [
        { articleId: 'soap', destinationId: 'store-id', quantity: 12 },
        { articleId: 'soap', destinationId: 'delivery-id', quantity: 5 },
      ],
    });
    expect(element.textContent).toContain('TRF-0093');
    expect(element.textContent).toContain('TRF-0094');
    expect(show).toHaveBeenCalledWith('day.open.validated', { count: 2 });
  });

  it('reprend les quantités du dernier jour ouvré et les envoie à la validation (JR-02)', async () => {
    openDay.mockReturnValue(of({ queued: false, body: OPENED }));
    await render();
    await type('qty-SALON-1', '2');

    yesterdayButton().click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(element.querySelector<HTMLInputElement>('#qty-SHOP-0')?.value).toBe('12');
    expect(element.querySelector<HTMLInputElement>('#qty-DELIVERY-0')?.value).toBe('5');
    expect(element.querySelector<HTMLInputElement>('#qty-SALON-1')?.value).toBe('0');
    await validate();
    expect(openDay).toHaveBeenCalledWith({
      lines: [
        { articleId: 'soap', destinationId: 'store-id', quantity: 12 },
        { articleId: 'soap', destinationId: 'delivery-id', quantity: 5 },
      ],
    });
  });

  it('désactive « Reprendre hier » la toute première journée (pas de veille)', async () => {
    await render({ ...GRID, previousDay: null });

    expect(yesterdayButton().disabled).toBe(true);
  });

  it('n’envoie rien quand on demande plus que le stock de l’entrepôt', async () => {
    await render();
    await type('qty-SHOP-1', '4');
    await type('qty-SALON-1', '3');

    await validate();

    expect(openDay).not.toHaveBeenCalled();
    expect(element.querySelector('tbody tr[data-level="error"]')).not.toBeNull();
  });

  it('affiche le refus du serveur (journée déjà ouverte)', async () => {
    openDay.mockReturnValue(
      throwError(() => new Error('La journée du 08/11/2026 est déjà ouverte.')),
    );
    await render();
    await type('qty-SHOP-0', '2');

    await validate();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('déjà ouverte');
    expect(validateButton().disabled).toBe(false);
  });

  it('met l’ouverture en file hors connexion et empêche un second envoi', async () => {
    openDay.mockReturnValue(of({ queued: true }));
    await render();
    const show = vi.spyOn(toast, 'show');
    await type('qty-SHOP-0', '2');

    await validate();

    expect(show).toHaveBeenCalledWith('common.queuedSaved', {}, 'info');
    expect(validateButton().disabled).toBe(true);
  });

  it('montre les bons du matin quand la journée est déjà ouverte', async () => {
    await render({
      ...GRID,
      vouchers: [{ number: 'TRF-0093', destination: 'SHOP', lineCount: 1, units: 12 }],
    });

    expect(element.textContent).toContain('TRF-0093');
    expect(element.querySelector('table')).toBeNull();
  });

  it('ne présente aucune violation d’accessibilité détectable', async () => {
    await render();
    document.body.appendChild(element);

    const results = await axe.run(element, {
      runOnly: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'],
      rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
    });

    expect(
      results.violations.map((violation) => `${violation.id}: ${violation.nodes[0]?.html}`),
    ).toEqual([]);
    element.remove();
  });
});
