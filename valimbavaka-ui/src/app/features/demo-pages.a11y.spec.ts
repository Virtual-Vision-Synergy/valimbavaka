import { Type } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import axe from 'axe-core';
import { AuthStore } from '../core/auth/auth-store';
import { buildUser } from '../core/auth/auth-testing';
import { provideTestTranslations } from '../core/i18n/i18n-testing';
import { DEMO_LATENCY_MS } from '../shared/data-access/demo';
import { DASHBOARD_TEST_VIEW } from './dashboard/data-access/dashboard-testing';

/** Pages alimentées par des données de démonstration : chacune se charge puis ne doit violer aucune règle axe. */
const PAGES: readonly (readonly [string, () => Promise<Type<unknown>>])[] = [
  [
    'dashboard',
    () => import('./dashboard/pages/dashboard-page/dashboard-page').then((m) => m.DashboardPage),
  ],
  [
    'commissions',
    () =>
      import('./commissions/pages/commissions-page/commissions-page').then(
        (m) => m.CommissionsPage,
      ),
  ],
  [
    'services',
    () => import('./services/pages/services-page/services-page').then((m) => m.ServicesPage),
  ],
  ['reports', () => import('./reports/pages/reports-page/reports-page').then((m) => m.ReportsPage)],
  [
    'production',
    () =>
      import('./production/pages/production-page/production-page').then((m) => m.ProductionPage),
  ],
  [
    'promotions',
    () =>
      import('./promotions/pages/promotion-list-page/promotion-list-page').then(
        (m) => m.PromotionListPage,
      ),
  ],
  [
    'deliveries',
    () =>
      import('./deliveries/pages/deliveries-page/deliveries-page').then((m) => m.DeliveriesPage),
  ],
];

const settle = (): Promise<void> =>
  new Promise((resolve) => setTimeout(resolve, DEMO_LATENCY_MS + 100));

/** Pages déjà branchées sur l'API (référentiels…) : chaque appel reçoit une page vide pour que la page se charge. */
const answerApiCalls = async (): Promise<void> => {
  await new Promise((resolve) => setTimeout(resolve));
  for (const request of TestBed.inject(HttpTestingController).match(() => true)) {
    request.flush(
<<<<<<< HEAD
      request.request.url === '/api/v1/reports'
        ? {
            granularity: 'MONTH',
            sales: { revenue: 0, salesCount: 0, averageBasket: 0 },
            comparison: {
              from: '2026-10-01',
              to: '2026-10-31',
              sales: { revenue: 0, salesCount: 0, averageBasket: 0 },
              variation: { revenue: null, salesCount: null, averageBasket: null },
            },
            trend: [
              { date: '2026-11-01', revenue: 0 },
              { date: '2026-11-02', revenue: 8500 },
            ],
          }
        : { items: [], page: 1, pageSize: 10, totalCount: 0, totalPages: 0 },
=======
      request.request.url === '/api/v1/dashboard'
        ? DASHBOARD_TEST_VIEW
        : request.request.url === '/api/v1/reports'
          ? {
              sales: { revenue: 0, salesCount: 0, averageBasket: 0 },
              comparison: {
                from: '2026-10-01',
                to: '2026-10-31',
                sales: { revenue: 0, salesCount: 0, averageBasket: 0 },
                variation: { revenue: null, salesCount: null, averageBasket: null },
              },
            }
          : { items: [], page: 1, pageSize: 10, totalCount: 0, totalPages: 0 },
>>>>>>> 99e8ae5f75eda53e93722a763d782ec202631630
    );
  }
};

describe('Demo pages accessibility', () => {
  it.each(PAGES)(
    '%s should have no detectable accessibility violation',
    async (_name, load) => {
      const component = await load();
      await TestBed.configureTestingModule({
        imports: [component],
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          provideRouter([]),
          provideTestTranslations(),
        ],
      }).compileComponents();
      TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
      const fixture = TestBed.createComponent(component);
      await answerApiCalls();
      await fixture.whenStable();
      await settle();
      await answerApiCalls();
      await fixture.whenStable();
      const host = fixture.nativeElement as HTMLElement;
      document.body.appendChild(host);

      const results = await axe.run(host, {
        runOnly: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'],
        rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
      });

      expect(
        results.violations.map((violation) => `${violation.id}: ${violation.nodes[0]?.html}`),
      ).toEqual([]);
      host.remove();
    },
    20_000,
  );
});
