import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { type MockInstance, vi } from 'vitest';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { SalonServiceRDto } from '../../data-access/salon-catalog-models';
import { SalonCatalogFormPage } from './salon-catalog-form-page';

const SERVICES_URL = '/api/v1/salon-services';
const ARTICLES_URL = '/api/v1/articles';
const COCONUT_OIL = '0195f0a0-0000-7000-8000-0000000000a1';
const CLAY_MASK = '0195f0a0-0000-7000-8000-0000000000a2';
const OLD_ARTICLE = '0195f0a0-0000-7000-8000-0000000000a9';

const article = (id: string, code: string, name: string, unit: string) => ({
  id,
  code,
  name,
  barcode: null,
  type: 'RAW_MATERIAL',
  rangeId: null,
  unit,
  cost: 12000,
  price: null,
  threshold: 0,
  supplierId: null,
  active: true,
  createdAt: '2026-11-01T08:00:00Z',
  updatedAt: '2026-11-01T08:00:00Z',
  version: 0,
});

const articles = [
  article(COCONUT_OIL, 'ART-0001', 'Huile de coco vierge', 'LITER'),
  article(CLAY_MASK, 'ART-0002', 'Masque argile rose 100 g', 'PIECE'),
];

const service: SalonServiceRDto = {
  id: '0195f0a0-0000-7000-8000-0000000000s1',
  name: 'Soin visage éclat',
  durationMinutes: 45,
  price: 35000,
  commissionRate: 12.5,
  products: [
    { articleId: COCONUT_OIL, quantity: 0.015 },
    { articleId: OLD_ARTICLE, quantity: 1 },
  ],
  active: true,
  createdAt: '2026-11-08T08:30:00Z',
  updatedAt: '2026-11-08T08:30:00Z',
  version: 2,
};

describe('SalonCatalogFormPage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<SalonCatalogFormPage>;
  let element: HTMLElement;
  let navigate: MockInstance<Router['navigate']>;

  const setup = async (id?: string): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [SalonCatalogFormPage],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN')); // les écritures passent par la file hors ligne
    httpMock = TestBed.inject(HttpTestingController);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(SalonCatalogFormPage);
    if (id) {
      fixture.componentRef.setInput('id', id);
    }
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    const articlesRequest = httpMock.expectOne((req) => req.url === ARTICLES_URL);
    expect(articlesRequest.request.params.get('active')).toBe('true');
    articlesRequest.flush({
      items: articles,
      page: 1,
      pageSize: 100,
      totalCount: 2,
      totalPages: 1,
    });
  };

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  const field = <T extends HTMLInputElement | HTMLSelectElement>(id: string): T =>
    element.querySelector<T>(`#${id}`) as T;

  const type = async (id: string, value: string): Promise<void> => {
    const control = field(id);
    control.value = value;
    control.dispatchEvent(new Event(control instanceof HTMLSelectElement ? 'change' : 'input'));
    await fixture.whenStable();
  };

  const click = async (label: RegExp): Promise<void> => {
    const button = Array.from(element.querySelectorAll<HTMLButtonElement>('button')).find(
      (candidate) => label.test(candidate.textContent ?? ''),
    );
    button?.click();
    await fixture.whenStable();
  };

  const save = async (): Promise<void> => {
    element.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();
    await fixture.whenStable();
  };

  const fillMain = async (): Promise<void> => {
    await type('name', 'Gommage corps café');
    await type('durationMinutes', '30');
    await type('price', '25000');
  };

  describe('in creation mode', () => {
    beforeEach(async () => {
      await setup();
      await fixture.whenStable();
    });

    it('should offer name, duration, price, commission and consumed products', () => {
      const labels = Array.from(element.querySelectorAll('label')).map((label) =>
        label.textContent?.trim(),
      );

      expect(labels).toEqual(['Nom *', 'Durée (min) *', 'Prix (Ar) *', 'Commission (%)']);
      expect(element.querySelector('legend')?.textContent).toContain('Produits consommés');
      expect(element.textContent).toContain('Aucun produit consommé.');
    });

    it('should create the service with its consumed products then go back to the catalogue', async () => {
      await fillMain();
      await type('commissionRate', '10');
      await click(/Ajouter un produit/);
      await type('article-0', CLAY_MASK);
      await type('quantity-0', '1');

      await save();

      const request = httpMock.expectOne(SERVICES_URL);
      expect(request.request.method).toBe('POST');
      expect(request.request.body).toEqual({
        name: 'Gommage corps café',
        durationMinutes: 30,
        price: 25000,
        commissionRate: 10,
        products: [{ articleId: CLAY_MASK, quantity: 1 }],
        version: null,
      });
      request.flush({ ...service, name: 'Gommage corps café' });
      await fixture.whenStable();
      expect(navigate).toHaveBeenCalledWith(['/services/catalog']);
    });

    it('should send a null commission rate when the field is left empty', async () => {
      await fillMain();

      await save();

      expect(httpMock.expectOne(SERVICES_URL).request.body).toMatchObject({
        commissionRate: null,
        products: [],
      });
    });

    it('should show the unit of the chosen article', async () => {
      await click(/Ajouter un produit/);
      await type('article-0', COCONUT_OIL);

      expect(element.querySelector('.unit')?.textContent).toContain('L');
    });

    it.each([
      ['durationMinutes', '0', '#durationMinutes-error', 'de 1 à 600 minutes'],
      ['durationMinutes', '601', '#durationMinutes-error', 'de 1 à 600 minutes'],
      ['commissionRate', '100.01', '#commissionRate-error', 'entre 0 et 100'],
      ['price', '-1', '#price-error', 'positif'],
    ])('should send nothing when %s is %s', async (id, value, errorId, message) => {
      await fillMain();
      await type(id, value);

      await save();

      httpMock.expectNone(SERVICES_URL);
      expect(element.querySelector(errorId)?.textContent).toContain(message);
    });

    it('should refuse the same article listed twice', async () => {
      await fillMain();
      await click(/Ajouter un produit/);
      await type('article-0', COCONUT_OIL);
      await type('quantity-0', '0.02');
      await click(/Ajouter un produit/);
      await type('article-1', COCONUT_OIL);
      await type('quantity-1', '0.01');

      await save();

      httpMock.expectNone(SERVICES_URL);
      expect(element.textContent).toContain('Choisissez un article différent par ligne');
    });

    it('should show the server message when the name is already used', async () => {
      await fillMain();

      await save();

      httpMock.expectOne(SERVICES_URL).flush(
        {
          title: 'CONFLICT',
          status: 409,
          code: 'CONFLICT',
          detail: 'Un service salon avec ce nom existe déjà.',
        },
        { status: 409, statusText: 'Conflict' },
      );
      await fixture.whenStable();

      expect(element.querySelector('[role="alert"]')?.textContent).toContain('existe déjà');
      expect(navigate).not.toHaveBeenCalled();
    });
  });

  describe('in edit mode', () => {
    beforeEach(async () => {
      await setup(service.id);
      httpMock.expectOne(`${SERVICES_URL}/${service.id}`).flush(service);
      await fixture.whenStable();
    });

    it('should fill the form and keep an article deactivated since', () => {
      expect(field<HTMLInputElement>('name').value).toBe('Soin visage éclat');
      expect(field<HTMLInputElement>('durationMinutes').value).toBe('45');
      expect(field<HTMLInputElement>('commissionRate').value).toBe('12.5');
      expect(field<HTMLSelectElement>('article-0').value).toBe(COCONUT_OIL);
      expect(field<HTMLSelectElement>('article-1').value).toBe(OLD_ARTICLE);
      expect(field<HTMLSelectElement>('article-1').selectedOptions[0].textContent).toContain(
        'Article inactif',
      );
    });

    it('should save with PUT, the version read and without the removed product', async () => {
      element.querySelectorAll<HTMLButtonElement>('.product-line button')[1].click();
      await fixture.whenStable();

      await save();

      const request = httpMock.expectOne(`${SERVICES_URL}/${service.id}`);
      expect(request.request.method).toBe('PUT');
      expect(request.request.body).toEqual({
        name: 'Soin visage éclat',
        durationMinutes: 45,
        price: 35000,
        commissionRate: 12.5,
        products: [{ articleId: COCONUT_OIL, quantity: 0.015 }],
        version: 2,
      });
      request.flush({ ...service, products: [service.products[0]], version: 3 });
    });
  });
});
