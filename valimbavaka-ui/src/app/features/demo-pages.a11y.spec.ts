import { Type } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import axe from 'axe-core';
import { AuthStore } from '../core/auth/auth-store';
import { buildUser } from '../core/auth/auth-testing';
import { provideTestTranslations } from '../core/i18n/i18n-testing';
import { DEMO_LATENCY_MS } from '../shared/data-access/demo';

/** Pages alimentées par des données de démonstration : chacune se charge puis ne doit violer aucune règle axe. */
const PAGES: readonly (readonly [string, () => Promise<Type<unknown>>])[] = [
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
    'transfers',
    () =>
      import('./transfers/pages/transfer-list-page/transfer-list-page').then(
        (m) => m.TransferListPage,
      ),
  ],
  [
    'inventory',
    () => import('./inventory/pages/inventory-page/inventory-page').then((m) => m.InventoryPage),
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
  ['day open', () => import('./day/pages/day-open-page/day-open-page').then((m) => m.DayOpenPage)],
  [
    'day close',
    () => import('./day/pages/day-close-page/day-close-page').then((m) => m.DayClosePage),
  ],
];

const settle = (): Promise<void> =>
  new Promise((resolve) => setTimeout(resolve, DEMO_LATENCY_MS + 100));

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
      await fixture.whenStable();
      await settle();
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
