import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ChannelDashboardRDto } from '../../data-access/channel-dashboard-models';
import { ChannelDashboardPage } from './channel-dashboard-page';

const URL = '/api/v1/sales/channel-dashboard';
const DASHBOARD: ChannelDashboardRDto = {
  from: '2026-11-01',
  to: '2026-11-30',
  totalRevenue: 100000,
  totalSalesCount: 7,
  channels: [
    {
      salesChannelId: 'c-store',
      salesChannelLabel: 'Boutique',
      revenue: 75000,
      salesCount: 5,
      revenueSharePercent: 75,
    },
    {
      salesChannelId: 'c-salon',
      salesChannelLabel: 'Salon',
      revenue: 0,
      salesCount: 0,
      revenueSharePercent: 0,
    },
  ],
};

/** Tableau de bord des ventes par canal (VTE-08) : CA, nombre de ventes et part du CA total de chaque canal. */
describe('ChannelDashboardPage', () => {
  let httpMock: HttpTestingController;

  const open = async (): Promise<HTMLElement> => {
    await TestBed.configureTestingModule({
      imports: [ChannelDashboardPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTestTranslations(),
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
    const fixture = TestBed.createComponent(ChannelDashboardPage);
    fixture.detectChanges();
    await fixture.whenStable();
    httpMock.expectOne((req) => req.url === URL).flush(DASHBOARD);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  };

  afterEach(() => httpMock.verify());

  it('should show the revenue, the number of sales and the share of each channel', async () => {
    const element = await open();

    const rows = Array.from(element.querySelectorAll('tbody tr')).map((row) =>
      (row.textContent ?? '').replace(/\s+/g, ' ').trim(),
    );
    expect(rows).toHaveLength(2);
    expect(rows[0]).toContain('Boutique');
    expect(rows[0]).toContain('5');
    expect(rows[0]).toContain('75 %');
    expect(rows[1]).toContain('Salon');
    expect(rows[1]).toContain('0 %');
  });

  it('should show the total number of sales of the period', async () => {
    const element = await open();

    expect(element.querySelector('.totals')?.textContent).toContain('7');
  });
});
