import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { StockPage } from './stock-page';

const page = (items: unknown[]) => ({
  items,
  page: 1,
  pageSize: 100,
  totalCount: items.length,
  totalPages: 1,
});

/** Un article de 10 unités à l'entrepôt au coût de 1 000 Ar : 0,01 M Ar si la valeur est calculée localement. */
const STOCK = {
  articleId: 'art-1',
  code: 'ART-0031',
  name: 'Savon noir 100 g',
  rangeId: null,
  unit: 'PIECE',
  threshold: 40,
  cost: 1000,
  total: 10,
  locations: [{ locationId: 'l1', code: 'WAREHOUSE', label: 'Entrepôt', quantity: 10 }],
};

/** Alerte de seuil telle que calculée par le serveur (STK-04). */
const ALERT = {
  articleId: 'art-1',
  code: 'ART-0031',
  name: 'Savon noir 100 g',
  unit: 'PIECE',
  quantity: 12.5,
  threshold: 40,
  shortage: 27.5,
  suggestedQuantity: 30,
};

/** Tout le stock vu par le serveur, articles hors de la page compris. */
const VALUATION = {
  quantity: 1250,
  value: 3_840_000,
  unvaluedQuantity: 12,
  locations: [
    {
      locationId: 'l1',
      code: 'WAREHOUSE',
      label: 'Entrepôt',
      quantity: 1250,
      value: 3_840_000,
      unvaluedQuantity: 12,
    },
  ],
};

/** STK-03 : cartes d'emplacement avec la quantité totale et la valeur en ariary de tout le stock. */
describe('StockPage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<StockPage>;
  let element: HTMLElement;

  const open = async (role: 'ADMIN' | 'SELLER', alerts: unknown[] = []): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [StockPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser(role));
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(StockPage);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    httpMock.expectOne((req) => req.url === '/api/v1/stocks').flush(page([STOCK]));
    httpMock.expectOne((req) => req.url === '/api/v1/product-ranges').flush(page([]));
    httpMock.expectOne((req) => req.url === '/api/v1/stocks/alerts').flush(page(alerts));
  };

  const settle = async (): Promise<void> => {
    await fixture.whenStable();
    fixture.detectChanges();
  };

  const card = (label: string): string =>
    Array.from(element.querySelectorAll<HTMLButtonElement>('vvs-location-cards button'))
      .find((button) => button.querySelector('.label')?.textContent?.trim() === label)
      ?.textContent?.replace(/\s+/g, ' ')
      .trim() ?? '';

  afterEach(() => httpMock.verify());

  it('affiche pour l’administrateur la quantité et la valeur de tout le stock calculées par le serveur', async () => {
    await open('ADMIN');
    httpMock.expectOne('/api/v1/stocks/valuation').flush(VALUATION);
    await settle();

    const warehouse = card('Entrepôt');
    expect(warehouse).toMatch(/1\s250 unités/);
    expect(warehouse).toContain('Valeur 3,84 M Ar');
    expect(warehouse).toContain("dont 12 sans coût d'achat");
    expect(card('Tous')).toContain('Valeur 3,84 M Ar');
  });

  it('garde le calcul sur les articles chargés si la valorisation échoue', async () => {
    await open('ADMIN');
    httpMock
      .expectOne('/api/v1/stocks/valuation')
      .flush(null, { status: 500, statusText: 'Server Error' });
    await settle();

    expect(card('Entrepôt')).toMatch(/10 unités/);
    expect(card('Entrepôt')).toContain('Valeur 0,01 M Ar');
  });

  it('affiche les articles sous le seuil avec stock, seuil, manque et quantité suggérée (STK-04)', async () => {
    await open('SELLER', [ALERT]);
    await settle();

    const row = Array.from(element.querySelectorAll('vvs-threshold-alerts tbody td'))
      .map((cell) => cell.textContent?.trim())
      .join(' | ');
    expect(element.querySelector('vvs-threshold-alerts h2')?.textContent).toContain('1 article(s)');
    expect(row).toContain('Savon noir 100 g');
    expect(row).toContain('12.5 | 40 | 27.5 | 30');
  });

  it("n'affiche pas le bloc d'alertes quand aucun article n'est sous son seuil", async () => {
    await open('SELLER');
    await settle();

    expect(element.querySelector('vvs-threshold-alerts')).toBeNull();
  });

  it('ne demande pas la valorisation pour une vendeuse (permission stocks.value)', async () => {
    await open('SELLER');
    await settle();

    httpMock.expectNone('/api/v1/stocks/valuation');
    expect(card('Entrepôt')).toMatch(/10 unités/);
    expect(card('Entrepôt')).not.toContain('sans coût');
  });
});
