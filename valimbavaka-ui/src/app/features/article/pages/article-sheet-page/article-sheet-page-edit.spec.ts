import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ArticleSheetPage } from './article-sheet-page';
import {
  ARTICLE_ID,
  ARTICLE_URL,
  RANGE_ID,
  SUPPLIER_ID,
  article,
  problem,
} from './article-sheet-page.fixtures';

/** ART-02 : ouverture d'une fiche existante (pré-remplissage) et garde-fou contre l'écrasement. */
describe('ArticleSheetPage opened on an existing article', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<ArticleSheetPage>;
  let element: HTMLElement;

  beforeEach(async () => {
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
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(ArticleSheetPage);
    fixture.componentRef.setInput('id', ARTICLE_ID);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

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
        items: [{ id: SUPPLIER_ID, name: 'Huiles Ravo', active: true }],
        page: 1,
        pageSize: 100,
        totalCount: 1,
        totalPages: 1,
      });
  };

  const field = <T extends HTMLInputElement | HTMLSelectElement>(id: string): T =>
    element.querySelector<T>(`#${id}`) as T;

  const submit = (): HTMLButtonElement =>
    element.querySelector<HTMLButtonElement>('button[type="submit"]') as HTMLButtonElement;

  const readSheet = async (sheet: object): Promise<void> => {
    httpMock.expectOne(ARTICLE_URL).flush(sheet);
    flushChoices();
    await fixture.whenStable();
    fixture.detectChanges();
  };

  it('should prefill a raw material without range, price, barcode or supplier', async () => {
    const rawMaterial = {
      ...article,
      type: 'RAW_MATERIAL',
      unit: 'KILOGRAM',
      rangeId: null,
      price: null,
      barcode: null,
      supplierId: null,
      cost: 12000,
      threshold: 5,
    };

    await readSheet(rawMaterial);

    expect(field<HTMLSelectElement>('type').value).toBe('RAW_MATERIAL');
    expect(field<HTMLSelectElement>('unit').value).toBe('KILOGRAM');
    expect(field<HTMLInputElement>('cost').value).toBe('12000');
    expect(field<HTMLInputElement>('threshold').value).toBe('5');
    expect(field<HTMLInputElement>('barcode').value).toBe('');
    expect(field<HTMLSelectElement>('supplierId').value).toBe('');
    expect(element.querySelector('#price')).toBeNull();
    expect(element.querySelector('#rangeId')).toBeNull();
  });

  it('should send back exactly the values read when nothing was changed', async () => {
    await readSheet(article);

    submit().click();
    await fixture.whenStable();

    const request = httpMock.expectOne(ARTICLE_URL);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({
      name: 'Savon noir 100 g',
      barcode: '3760123456789',
      type: 'FINISHED',
      rangeId: RANGE_ID,
      unit: 'PIECE',
      cost: 3120.5,
      price: 8500,
      threshold: 40,
      supplierId: SUPPLIER_ID,
      version: 3,
    });
    request.flush(article);
  });

  it('should keep the save button disabled until the sheet is read', async () => {
    expect(submit().disabled).toBe(true);

    await readSheet(article);

    expect(submit().disabled).toBe(false);
  });

  it('should send nothing when the sheet could not be read', async () => {
    httpMock
      .expectOne(ARTICLE_URL)
      .flush(problem(500, 'INTERNAL_ERROR', 'Erreur interne du serveur.'), {
        status: 500,
        statusText: 'Server Error',
      });
    flushChoices();
    await fixture.whenStable();
    fixture.detectChanges();
    field<HTMLInputElement>('name').value = 'Savon écrasé';
    field<HTMLInputElement>('name').dispatchEvent(new Event('input'));

    submit().click();
    await fixture.whenStable();

    expect(element.querySelector('[role="alert"]')).not.toBeNull();
    expect(submit().disabled).toBe(true);
    httpMock.expectNone(ARTICLE_URL);
  });
});
