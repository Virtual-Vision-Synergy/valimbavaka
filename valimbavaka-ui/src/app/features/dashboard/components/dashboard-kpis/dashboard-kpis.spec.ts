import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { formatAriary } from '../../../../shared/pipes/ariary-pipe';
import { DashboardRDto } from '../../data-access/dashboard-models';
import { DASHBOARD_TEST_VIEW } from '../../data-access/dashboard-testing';
import { DashboardKpis } from './dashboard-kpis';

describe('DashboardKpis', () => {
  let fixture: ComponentFixture<DashboardKpis>;
  let element: HTMLElement;

  const render = async (data: DashboardRDto): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [DashboardKpis],
      providers: [provideRouter([]), provideTestTranslations()],
    }).compileComponents();
    fixture = TestBed.createComponent(DashboardKpis);
    fixture.componentRef.setInput('data', data);
    element = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  };

  const cards = (): HTMLElement[] => Array.from(element.querySelectorAll('vvs-kpi-card'));

  it('affiche les 4 indicateurs clés sur un seul écran', async () => {
    await render(DASHBOARD_TEST_VIEW);

    expect(cards()).toHaveLength(4);
    const text = element.textContent ?? '';
    expect(text).toContain(formatAriary(1_284_000, 'plain'));
    expect(text).toContain('47');
    expect(text).toContain('9 / 14');
    expect(text).toContain('−4');
  });

  it('compare le chiffre d’affaires et les ventes avec hier', async () => {
    await render(DASHBOARD_TEST_VIEW);

    const [revenue, sales] = cards();
    expect(revenue.textContent).toContain('+12,4 %');
    expect(sales.textContent).toContain('+5');
  });

  it('n’affiche pas de tendance du chiffre d’affaires sans base de comparaison', async () => {
    await render({
      ...DASHBOARD_TEST_VIEW,
      revenue: { amount: 8_500, previousAmount: 0, changePercent: null },
    });

    expect(cards()[0].textContent).not.toContain('%');
  });

  it('montre une baisse avec un vrai signe moins', async () => {
    await render({
      ...DASHBOARD_TEST_VIEW,
      revenue: { amount: 6_000, previousAmount: 9_000, changePercent: -33.3 },
    });

    expect(cards()[0].textContent).toContain('−33,3 %');
  });

  it('montre les échecs de livraison et les annulées hors total', async () => {
    await render(DASHBOARD_TEST_VIEW);

    const deliveries = cards()[2];
    expect(deliveries.textContent).toContain('2 échecs');
    expect(deliveries.textContent).toContain('1 annulée');
  });

  it('liste chaque canal avec son nombre de ventes, même un canal désactivé', async () => {
    await render({
      ...DASHBOARD_TEST_VIEW,
      sales: {
        ...DASHBOARD_TEST_VIEW.sales,
        channels: [
          ...DASHBOARD_TEST_VIEW.sales.channels,
          {
            salesChannelId: '7f1c2a9e-0000-4000-8000-000000000003',
            salesChannelLabel: null,
            revenue: 0,
            salesCount: 0,
            revenueSharePercent: 0,
          },
        ],
      },
    });

    const legend = Array.from(cards()[1].querySelectorAll('.legend li')).map((item) =>
      item.textContent?.trim(),
    );
    expect(legend).toHaveLength(3);
    expect(legend[0]).toBe('Boutique 26');
    expect(legend[1]).toBe('Facebook 21');
  });

  it('n’a pas d’écart de stock tant que la journée n’est pas ouverte', async () => {
    await render({ ...DASHBOARD_TEST_VIEW, status: null, stockGap: null });

    const gap = cards()[3];
    expect(gap.textContent).toContain('—');
    expect(gap.textContent).not.toContain('À justifier');
  });

  it('ne signale rien quand l’écart de stock est nul', async () => {
    await render({ ...DASHBOARD_TEST_VIEW, stockGap: { units: 0, articles: 0 } });

    expect(cards()[3].textContent).not.toContain('À justifier');
  });
});
