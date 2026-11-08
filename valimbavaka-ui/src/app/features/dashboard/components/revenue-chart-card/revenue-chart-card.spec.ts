import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { formatAriary } from '../../../../shared/pipes/ariary-pipe';
import { RevenueTrendRDto } from '../../data-access/dashboard-models';
import { RevenueChartCard, niceStep } from './revenue-chart-card';

/** Réponse de `GET /api/v1/dashboard/revenue-trend` le dimanche 8 novembre 2026. */
const TREND: RevenueTrendRDto = {
  today: '2026-11-08',
  last7Days: [
    { date: '2026-11-02', amount: 980_000 },
    { date: '2026-11-03', amount: 0 },
    { date: '2026-11-04', amount: 1_460_000 },
    { date: '2026-11-05', amount: 125_000 },
    { date: '2026-11-06', amount: 640_000 },
    { date: '2026-11-07', amount: 1_050_000 },
    { date: '2026-11-08', amount: 79_000 },
  ],
  last4Weeks: [
    { date: '2026-10-12', amount: 6_420_000 },
    { date: '2026-10-19', amount: 7_180_000 },
    { date: '2026-10-26', amount: 0 },
    { date: '2026-11-02', amount: 4_334_000 },
  ],
};

/** DSH-02 : une barre par jour (ou par semaine) avec une infobulle donnant la date et le montant exact. */
describe('RevenueChartCard', () => {
  let fixture: ComponentFixture<RevenueChartCard>;
  let element: HTMLElement;

  const render = async (inputs: { trend?: RevenueTrendRDto; failed?: boolean }): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [RevenueChartCard],
      providers: [provideTestTranslations()],
    }).compileComponents();
    fixture = TestBed.createComponent(RevenueChartCard);
    fixture.componentRef.setInput('trend', inputs.trend);
    fixture.componentRef.setInput('failed', inputs.failed ?? false);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    element = fixture.nativeElement as HTMLElement;
  };

  const columns = (): HTMLElement[] => Array.from(element.querySelectorAll<HTMLElement>('.column'));
  const tooltips = (): string[] => columns().map((column) => tooltipOf(column));
  const tooltipOf = (column: HTMLElement): string =>
    column.querySelector('.tooltip')?.textContent?.trim() ?? '';

  it('affiche une barre par jour sur 7 jours, aujourd’hui en dernier et mis en avant', async () => {
    await render({ trend: TREND });

    expect(columns()).toHaveLength(7);
    expect(columns()[6].classList).toContain('highlight');
    expect(columns()[6].querySelector('.label')?.textContent?.trim()).toBe("Aujourd'hui");
    expect(columns().filter((column) => column.classList.contains('highlight'))).toHaveLength(1);
  });

  it('donne dans l’infobulle de chaque jour sa date complète et son montant exact', async () => {
    await render({ trend: TREND });

    expect(tooltips()[3]).toBe(`jeudi 5 novembre : ${formatAriary(125_000)}`);
    expect(tooltips()[1]).toBe(`mardi 3 novembre : ${formatAriary(0)}`);
    expect(tooltips()[6]).toBe(`dimanche 8 novembre : ${formatAriary(79_000)}`);
  });

  it('garde une barre à 0 pour un jour sans vente', async () => {
    await render({ trend: TREND });

    const table = Array.from(element.querySelectorAll('table.sr-only tbody tr'));
    expect(table).toHaveLength(7);
    expect(table[1].querySelector('td')?.textContent?.trim()).toBe(formatAriary(0, 'compact'));
  });

  it('résume le total et la moyenne journalière des 7 jours', async () => {
    await render({ trend: TREND });

    const subtitle = element.querySelector('.card-subtitle')?.textContent ?? '';
    expect(subtitle).toContain(formatAriary(4_334_000));
    expect(subtitle).toContain(formatAriary(619_143));
  });

  it('passe à 4 barres hebdomadaires avec « Semaine du … » dans l’infobulle', async () => {
    await render({ trend: TREND });

    const month = element.querySelectorAll<HTMLInputElement>('input[type="radio"]')[1];
    month.click();
    fixture.detectChanges();

    expect(columns()).toHaveLength(4);
    expect(tooltips()[0]).toBe(`Semaine du 12 octobre : ${formatAriary(6_420_000)}`);
    expect(columns()[3].querySelector('.label')?.textContent?.trim()).toBe('Cette semaine');
  });

  it('montre un squelette tant que la tendance n’est pas chargée', async () => {
    await render({});

    expect(columns()).toHaveLength(0);
    expect(element.querySelector('[role="status"]')).not.toBeNull();
    expect(element.querySelector('.card-subtitle')).toBeNull();
  });

  it('montre une erreur avec réessai si la lecture échoue', async () => {
    await render({ failed: true });
    let retried = 0;
    fixture.componentInstance.retry.subscribe(() => retried++);

    element.querySelector<HTMLButtonElement>('[role="alert"] button')?.click();

    expect(columns()).toHaveLength(0);
    expect(retried).toBe(1);
  });
});

describe('niceStep', () => {
  it('choisit un pas rond donnant environ quatre graduations', () => {
    expect(niceStep(1_710_000)).toBe(500_000);
    expect(niceStep(7_640_000)).toBe(2_000_000);
    expect(niceStep(25_500)).toBe(10_000);
  });

  it('garde un pas minimal de 1 000 Ar quand rien n’a été vendu', () => {
    expect(niceStep(0)).toBe(1_000);
  });
});
