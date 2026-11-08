import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { PageResponse } from '../../../../shared/models/page-response';
import { StockAlertRDto } from '../../../stocks/data-access/stock-models';
import { StockAlertsCard } from './stock-alerts-card';

const alert = (overrides: Partial<StockAlertRDto>): StockAlertRDto => ({
  articleId: '7f1c2a9e-0000-4000-8000-000000000031',
  code: 'ART-0031',
  name: 'Savon noir 100 g',
  unit: 'PIECE',
  quantity: 12.5,
  threshold: 40,
  shortage: 27.5,
  suggestedQuantity: 30,
  supplierId: null,
  supplierName: null,
  leadTimeDays: null,
  expectedDeliveryDate: null,
  ...overrides,
});

const page = (
  items: StockAlertRDto[],
  totalCount = items.length,
): PageResponse<StockAlertRDto> => ({
  items,
  page: 1,
  pageSize: 100,
  totalCount,
  totalPages: 1,
});

/** Ordre du serveur (STK-04) : rupture d'abord, puis couverture croissante. */
const SERVER_ORDER = [
  alert({
    articleId: 'a-0112',
    code: 'ART-0112',
    name: 'Huile de coco vierge 250 ml',
    quantity: 0,
  }),
  alert({ articleId: 'a-0087', code: 'ART-0087', name: 'Gommage café 200 g', quantity: 3 }),
  alert({}),
  alert({ articleId: 'a-0154', code: 'ART-0154', name: 'Beurre de karité 150 g', quantity: 30 }),
];

/** DSH-03 : alertes de stock du cockpit, chaque article mène à sa fiche détaillée. */
describe('StockAlertsCard', () => {
  let fixture: ComponentFixture<StockAlertsCard>;
  let element: HTMLElement;

  const render = async (
    inputs: { page?: PageResponse<StockAlertRDto>; failed?: boolean; limit?: number },
    role: 'ADMIN' | 'SELLER' = 'ADMIN',
  ): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [StockAlertsCard],
      providers: [provideRouter([]), provideTestTranslations()],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser(role));
    fixture = TestBed.createComponent(StockAlertsCard);
    fixture.componentRef.setInput('page', inputs.page);
    fixture.componentRef.setInput('failed', inputs.failed ?? false);
    fixture.componentRef.setInput('limit', inputs.limit);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    element = fixture.nativeElement as HTMLElement;
  };

  const articleLinks = (): HTMLAnchorElement[] =>
    Array.from(element.querySelectorAll<HTMLAnchorElement>('tbody a'));

  it('rend chaque article en alerte cliquable vers sa fiche détaillée', async () => {
    await render({ page: page([alert({})]) });

    const links = articleLinks();
    expect(links.map((link) => link.textContent?.trim())).toEqual(['Savon noir 100 g']);
    expect(links[0].getAttribute('href')).toBe('/articles/7f1c2a9e-0000-4000-8000-000000000031');
  });

  it('affiche stock, seuil, quantité à commander avec son unité et le code article', async () => {
    await render({ page: page([alert({})]) });

    const cells = Array.from(element.querySelectorAll('tbody td')).map((cell) =>
      cell.textContent?.replace(/\s+/g, ' ').trim(),
    );
    expect(cells[0]).toContain('ART-0031');
    expect(cells[1]).toBe('12.5 / 40');
    expect(cells[2]).toBe('30 Pièce');
    expect(cells[3]).toBe('Stock faible');
  });

  it('garde l’ordre du serveur, signale la rupture et limite la vue d’ensemble aux 3 plus urgents', async () => {
    await render({ page: page(SERVER_ORDER, 7), limit: 3 });

    expect(articleLinks().map((link) => link.textContent?.trim())).toEqual([
      'Huile de coco vierge 250 ml',
      'Gommage café 200 g',
      'Savon noir 100 g',
    ]);
    expect(element.querySelector('tbody tr')?.textContent).toContain('Rupture');
    expect(element.querySelector('.card-subtitle')?.textContent?.trim()).toBe(
      '7 article(s) sous le seuil minimum',
    );
  });

  it('affiche toutes les alertes reçues sans limite', async () => {
    await render({ page: page(SERVER_ORDER) });

    expect(articleLinks()).toHaveLength(4);
  });

  it('indique qu’aucun article n’est sous son seuil quand la liste est vide', async () => {
    await render({ page: page([]) });

    expect(element.querySelector('table')).toBeNull();
    expect(element.textContent).toContain('Aucun article sous son seuil');
    expect(element.querySelector('.card-subtitle')?.textContent?.trim()).toBe(
      '0 article(s) sous le seuil minimum',
    );
  });

  it('affiche un état d’erreur avec réessai quand le chargement échoue', async () => {
    await render({ failed: true });
    const retried: unknown[] = [];
    fixture.componentInstance.retry.subscribe(() => retried.push(true));

    const retry = element.querySelector<HTMLButtonElement>('[role="alert"] button');
    retry?.click();

    expect(element.querySelector('table')).toBeNull();
    expect(retried).toHaveLength(1);
  });

  it('affiche un squelette tant que la page n’est pas reçue', async () => {
    await render({});

    expect(element.querySelector('[role="status"]')).not.toBeNull();
    expect(element.querySelector('.card-subtitle')).toBeNull();
  });

  it('réserve le raccourci de réapprovisionnement à stocks.restock (administrateur)', async () => {
    await render({ page: page([alert({})]) }, 'SELLER');

    const actions = Array.from(element.querySelectorAll('.actions a')).map((link) =>
      link.textContent?.trim(),
    );
    expect(actions).toEqual(['Tout voir']);
  });

  it('propose le raccourci de réapprovisionnement à l’administrateur', async () => {
    await render({ page: page([alert({})]) });

    const actions = Array.from(element.querySelectorAll('.actions a')).map((link) =>
      link.textContent?.trim(),
    );
    expect(actions).toEqual(['Tout voir', 'Générer le bon de réappro.']);
  });
});
