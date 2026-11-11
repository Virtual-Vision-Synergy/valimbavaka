import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { DEMO_LATENCY_MS } from '../../../../shared/data-access/demo';
import { DASHBOARD_TEST_VIEW } from '../../data-access/dashboard-testing';
import { DashboardPage } from './dashboard-page';

const ALERTS_URL = '/api/v1/stocks/alerts';
const DASHBOARD_URL = '/api/v1/dashboard';

/** Alerte de seuil telle que calculée par le serveur (STK-04). */
const ALERT = {
  articleId: '7f1c2a9e-0000-4000-8000-000000000031',
  code: 'ART-0031',
  name: 'Savon noir 100 g',
  unit: 'PIECE',
  quantity: 0,
  threshold: 40,
  shortage: 40,
  suggestedQuantity: 40,
  supplierId: null,
  supplierName: null,
  leadTimeDays: null,
  expectedDeliveryDate: null,
};

const page = (items: unknown[]) => ({
  items,
  page: 1,
  pageSize: 100,
  totalCount: items.length,
  totalPages: 1,
});

/** Laisse passer la latence des zones encore simulées du cockpit (graphiques, flux d'activité…). */
const settle = (): Promise<void> =>
  new Promise((resolve) => setTimeout(resolve, DEMO_LATENCY_MS + 100));

/**
 * DSH-01 : les 4 indicateurs viennent de l'API du cockpit, pour l'administrateur seul. DSH-03 : les alertes de seuil
 * viennent de l'API des stocks, indépendamment de l'agrégat du cockpit.
 */
describe('DashboardPage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<DashboardPage>;
  let element: HTMLElement;

  const open = async (role: 'ADMIN' | 'SELLER' | 'DELIVERY_DRIVER'): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser(role));
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DashboardPage);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  };

  /** Répond à l'agrégat du cockpit (administrateur seulement, les autres rôles ne l'appellent pas). */
  const answerDashboard = (): void => {
    httpMock.expectOne((req) => req.url === DASHBOARD_URL).flush(DASHBOARD_TEST_VIEW);
  };

  const render = async (): Promise<void> => {
    await settle();
    await fixture.whenStable();
    fixture.detectChanges();
  };

  afterEach(() => httpMock.verify());

  it('lit les 100 alertes les plus urgentes et rend chaque article cliquable vers sa fiche', async () => {
    await open('ADMIN');

    answerDashboard();
    const request = httpMock.expectOne((req) => req.url === ALERTS_URL);
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('100');
    request.flush(page([ALERT]));
    await render();

    const link = element.querySelector<HTMLAnchorElement>('vvs-stock-alerts-card tbody a');
    expect(link?.textContent?.trim()).toBe('Savon noir 100 g');
    expect(link?.getAttribute('href')).toBe('/articles/7f1c2a9e-0000-4000-8000-000000000031');
    expect(element.querySelector('vvs-stock-alerts-card')?.textContent).toContain('Rupture');
  });

  it('montre aux alertes seules la vendeuse, qui a stocks.read mais pas reports.read', async () => {
    await open('SELLER');

    httpMock.expectNone((req) => req.url === DASHBOARD_URL);
    httpMock.expectOne((req) => req.url === ALERTS_URL).flush(page([ALERT]));
    await render();

    expect(element.querySelectorAll('vvs-stock-alerts-card tbody tr')).toHaveLength(1);
    expect(element.querySelector('vvs-dashboard-kpis')).toBeNull();
    expect(element.querySelector('.restricted')).not.toBeNull();
  });

  it('n’appelle aucune API et ne montre aucun indicateur au livreur', async () => {
    await open('DELIVERY_DRIVER');
    await render();

    httpMock.expectNone((req) => req.url === ALERTS_URL);
    httpMock.expectNone((req) => req.url === DASHBOARD_URL);
    expect(element.querySelector('vvs-stock-alerts-card')).toBeNull();
    expect(element.querySelector('vvs-dashboard-kpis')).toBeNull();
    expect(element.querySelector('.restricted')).not.toBeNull();
  });

  it('affiche les 4 indicateurs lus sur l’API et la date de la journée du serveur', async () => {
    await open('ADMIN');

    answerDashboard();
    httpMock.expectOne((req) => req.url === ALERTS_URL).flush(page([]));
    await render();

    expect(element.querySelectorAll('vvs-dashboard-kpis vvs-kpi-card')).toHaveLength(4);
    expect(element.querySelector('vvs-dashboard-kpis')?.textContent).toContain('9 / 14');
    expect(element.querySelector('vvs-dashboard-hero')?.textContent).toContain('11 novembre 2026');
    expect(element.querySelector('vvs-stepper')?.textContent).toContain('47 tickets');
  });

  it('propose de réessayer quand l’agrégat du cockpit échoue', async () => {
    await open('ADMIN');

    httpMock
      .expectOne((req) => req.url === DASHBOARD_URL)
      .flush(null, { status: 503, statusText: 'Service Unavailable' });
    httpMock.expectOne((req) => req.url === ALERTS_URL).flush(page([]));
    await render();

    expect(element.querySelector('vvs-dashboard-kpis')).toBeNull();
    expect(element.querySelector('[role="alert"]')).not.toBeNull();

    element.querySelector<HTMLButtonElement>('[role="alert"] button')?.click();
    TestBed.tick();
    answerDashboard();
    await render();

    expect(element.querySelector('vvs-dashboard-kpis')).not.toBeNull();
  });

  it('affiche une erreur dans le bloc sans bloquer le cockpit, puis recharge au réessai', async () => {
    await open('ADMIN');

    answerDashboard();
    httpMock
      .expectOne((req) => req.url === ALERTS_URL)
      .flush(null, { status: 503, statusText: 'Service Unavailable' });
    await render();

    const card = element.querySelector('vvs-stock-alerts-card');
    expect(card?.querySelector('[role="alert"]')).not.toBeNull();
    expect(element.querySelector('vvs-dashboard-kpis')).not.toBeNull();

    card?.querySelector<HTMLButtonElement>('[role="alert"] button')?.click();
    TestBed.tick();
    httpMock.expectOne((req) => req.url === ALERTS_URL).flush(page([]));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(card?.textContent).toContain('Aucun article sous son seuil');
  });
});
