import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { type MockInstance, vi } from 'vitest';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ArticleSheetPage } from './article-sheet-page';
import {
  ARTICLES_URL,
  ARTICLE_ID,
  ARTICLE_URL,
  RANGE_ID,
  SUPPLIER_ID,
  article,
  problem,
} from './article-sheet-page.fixtures';

describe('ArticleSheetPage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<ArticleSheetPage>;
  let element: HTMLElement;
  let navigate: MockInstance<Router['navigate']>;

  const setup = async (id?: string): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [ArticleSheetPage],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(ArticleSheetPage);
    if (id) {
      fixture.componentRef.setInput('id', id);
    }
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  };

  /** Listes de choix lues à l'ouverture : gammes (référentiel) et fournisseurs. */
  const flushChoices = (): void => {
    httpMock
      .expectOne((req) => req.url === '/api/v1/product-ranges')
      .flush({
        items: [{ id: RANGE_ID, code: 'BOTANIKA', label: 'Botanika' }],
        page: 1,
        pageSize: 100,
        totalCount: 1,
        totalPages: 1,
      });
    httpMock
      .expectOne((req) => req.url === '/api/v1/suppliers')
      .flush({
        items: [
          { id: SUPPLIER_ID, name: 'Huiles Ravo', active: true },
          { id: 'inactive', name: 'Ancien fournisseur', active: false },
        ],
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

  const save = async (): Promise<void> => {
    element.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();
    await fixture.whenStable();
  };

  describe('in edit mode', () => {
    beforeEach(async () => {
      await setup(ARTICLE_ID);
      httpMock.expectOne(ARTICLE_URL).flush(article);
      flushChoices();
      await fixture.whenStable();
      fixture.detectChanges();
    });

    it('should show the code and fill every field with the sheet read from the server', () => {
      expect(element.querySelector('#article-code')?.textContent).toContain('ART-0031');
      expect(field<HTMLInputElement>('name').value).toBe('Savon noir 100 g');
      expect(field<HTMLSelectElement>('type').value).toBe('FINISHED');
      expect(field<HTMLSelectElement>('unit').value).toBe('PIECE');
      expect(field<HTMLInputElement>('price').value).toBe('8500');
      expect(field<HTMLInputElement>('cost').value).toBe('3120.5');
      expect(field<HTMLInputElement>('threshold').value).toBe('40');
      expect(field<HTMLInputElement>('barcode').value).toBe('3760123456789');
    });

    it('should only offer active suppliers, plus the one already linked', () => {
      const names = Array.from(field<HTMLSelectElement>('supplierId').options).map((o) =>
        o.textContent?.trim(),
      );

      expect(names).toContain('Huiles Ravo');
      expect(names).not.toContain('Ancien fournisseur');
    });

    it('should save with PUT, sending the version read, then go back to the list', async () => {
      await type('name', 'Savon noir 120 g');
      await type('threshold', '25');

      await save();

      const request = httpMock.expectOne(ARTICLE_URL);
      expect(request.request.method).toBe('PUT');
      expect(request.request.body).toEqual({
        name: 'Savon noir 120 g',
        barcode: '3760123456789',
        type: 'FINISHED',
        rangeId: RANGE_ID,
        unit: 'PIECE',
        cost: 3120.5,
        price: 8500,
        threshold: 25,
        supplierId: SUPPLIER_ID,
        version: 3,
      });
      request.flush({ ...article, name: 'Savon noir 120 g', version: 4 });
      await fixture.whenStable();
      expect(navigate).toHaveBeenCalledWith(['/articles']);
    });

    it('should hide the range and the price and send them as null for a raw material', async () => {
      await type('type', 'RAW_MATERIAL');
      fixture.detectChanges();

      expect(element.querySelector('#rangeId')).toBeNull();
      expect(element.querySelector('#price')).toBeNull();

      await save();

      expect(httpMock.expectOne(ARTICLE_URL).request.body).toMatchObject({
        type: 'RAW_MATERIAL',
        rangeId: null,
        price: null,
      });
    });

    it('should show the server message when the sheet was modified in the meantime', async () => {
      await save();

      httpMock
        .expectOne(ARTICLE_URL)
        .flush(
          problem(
            409,
            'CONFLICT',
            "L'article a été modifié entre-temps : rechargez la fiche puis réessayez.",
          ),
          { status: 409, statusText: 'Conflict' },
        );
      await fixture.whenStable();

      expect(element.textContent).toContain('modifié entre-temps');
      expect(navigate).not.toHaveBeenCalled();
      expect(field<HTMLInputElement>('name').value).toBe('Savon noir 100 g');
    });

    it('should show the server message when the barcode is already used', async () => {
      await save();

      httpMock
        .expectOne(ARTICLE_URL)
        .flush(problem(409, 'CONFLICT', 'Un article avec ce code-barres existe déjà.'), {
          status: 409,
          statusText: 'Conflict',
        });
      await fixture.whenStable();

      expect(element.querySelector('[role="alert"]')?.textContent).toContain('code-barres');
    });

    it('should disable the save button while saving so that a double click sends one request', async () => {
      await save();
      await save();

      const button = element.querySelector<HTMLButtonElement>('button[type="submit"]');
      expect(button?.disabled).toBe(true);
      httpMock.expectOne(ARTICLE_URL).flush(article);
      await fixture.whenStable();
      expect(button?.disabled).toBe(false);
    });

    it('should send nothing and ask for a name when the name is only spaces', async () => {
      await type('name', '   ');

      await save();

      httpMock.expectNone(ARTICLE_URL);
      expect(element.querySelector('#name-error')?.textContent).toContain(
        'Ce champ est obligatoire',
      );
    });

    it('should send nothing when the name exceeds 120 characters, and accept exactly 120', async () => {
      await type('name', 'a'.repeat(121));
      await save();
      httpMock.expectNone(ARTICLE_URL);
      expect(element.querySelector('#name-error')?.textContent).toContain('120 caractères');

      await type('name', 'a'.repeat(120));
      await save();
      expect(httpMock.expectOne(ARTICLE_URL).request.body).toMatchObject({
        name: 'a'.repeat(120),
      });
    });

    it.each(['12345', 'abcdefghijklm', '37601234567890'])(
      'should send nothing when the barcode is %s',
      async (barcode) => {
        await type('barcode', barcode);

        await save();

        httpMock.expectNone(ARTICLE_URL);
        expect(element.querySelector('#barcode-error')?.textContent).toContain('13 chiffres');
      },
    );

    it('should send an empty barcode as null', async () => {
      await type('barcode', '');

      await save();

      expect(httpMock.expectOne(ARTICLE_URL).request.body).toMatchObject({ barcode: null });
    });

    it('should refuse a negative cost, price or threshold and accept zero', async () => {
      await type('cost', '-0.01');
      await type('price', '-1');
      await type('threshold', '-5');
      await save();
      httpMock.expectNone(ARTICLE_URL);
      expect(element.querySelector('#cost-error')?.textContent).toContain('négative');
      expect(element.querySelector('#price-error')?.textContent).toContain('négative');
      expect(element.querySelector('#threshold-error')?.textContent).toContain('négative');

      await type('cost', '0');
      await type('price', '0');
      await type('threshold', '0');
      await save();
      expect(httpMock.expectOne(ARTICLE_URL).request.body).toMatchObject({
        cost: 0,
        price: 0,
        threshold: 0,
      });
    });
  });

  describe('in creation mode', () => {
    beforeEach(async () => {
      await setup();
      flushChoices();
      await fixture.whenStable();
    });

    it('should not read any sheet and show no code yet', () => {
      expect(element.querySelector('#article-code')).toBeNull();
    });

    it('should create with POST, without version, then go back to the list', async () => {
      await type('name', '  Gommage café 200 g ');
      await type('rangeId', RANGE_ID);
      await type('price', '12500');

      await save();

      const request = httpMock.expectOne(ARTICLES_URL);
      expect(request.request.method).toBe('POST');
      expect(request.request.body).toMatchObject({
        name: '  Gommage café 200 g ',
        type: 'FINISHED',
        price: 12500,
        threshold: 0,
        version: null,
      });
      request.flush({ ...article, version: 0 }, { status: 201, statusText: 'Created' });
      await fixture.whenStable();
      expect(navigate).toHaveBeenCalledWith(['/articles']);
    });

    it('should send nothing when a finished product has no range or no price', async () => {
      await type('name', 'Gommage café 200 g');

      await save();

      httpMock.expectNone(ARTICLES_URL);
      expect(element.querySelector('#rangeId-error')).not.toBeNull();
      expect(element.querySelector('#price-error')).not.toBeNull();
    });

    it('should create a raw material without range or price', async () => {
      await type('name', 'Beurre de karité brut');
      await type('type', 'RAW_MATERIAL');
      await type('unit', 'KILOGRAM');

      await save();

      expect(httpMock.expectOne(ARTICLES_URL).request.body).toMatchObject({
        type: 'RAW_MATERIAL',
        unit: 'KILOGRAM',
        rangeId: null,
        price: null,
      });
    });
  });

  describe('when the sheet does not exist', () => {
    it('should show the server message', async () => {
      await setup(ARTICLE_ID);

      httpMock
        .expectOne(ARTICLE_URL)
        .flush(problem(404, 'NOT_FOUND', `Article introuvable : ${ARTICLE_ID}`), {
          status: 404,
          statusText: 'Not Found',
        });
      flushChoices();
      await fixture.whenStable();

      expect(element.querySelector('[role="alert"]')?.textContent).toContain('Article introuvable');
    });
  });
});
