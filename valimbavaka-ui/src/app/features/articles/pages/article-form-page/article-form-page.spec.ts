import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { FormHarness, mountForm, online } from './article-form-page.harness';
import {
  ARTICLES_URL,
  RANGE_ID,
  SUPPLIER_ID,
  article,
  problem,
} from './article-form-page.fixtures';

/** Création d'un article avec le formulaire de la maquette (ART-01, ART-02). */
describe('ArticleFormPage in creation mode', () => {
  let h: FormHarness;

  beforeEach(async () => {
    h = await mountForm();
    h.flushChoices();
    await h.fixture.whenStable();
    h.fixture.detectChanges();
  });

  afterEach(() => {
    try {
      h.httpMock.verify();
    } finally {
      vi.restoreAllMocks();
      TestBed.resetTestingModule();
    }
  });

  const fillValidFinishedProduct = async (): Promise<void> => {
    await h.type('article-name', '  Gommage café 200 g ');
    await h.choose('rangeId', 'Botanika');
    await h.type('article-price', '12500');
  };

  it('should show no code yet', () => {
    expect(h.field<HTMLInputElement>('article-reference').value).toBe('');
  });

  it('should offer the ranges and the active suppliers read from the server', () => {
    const suppliers = Array.from(h.field<HTMLSelectElement>('article-supplier').options).map((o) =>
      o.textContent?.trim(),
    );

    expect(h.element.textContent).toContain('Botanika');
    expect(suppliers).toContain('Huiles Ravo');
    expect(suppliers).not.toContain('Ancien fournisseur');
  });

  it('should create with POST, without version, then go back to the list', async () => {
    await fillValidFinishedProduct();

    await h.save();

    const request = h.httpMock.expectOne(ARTICLES_URL);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      name: 'Gommage café 200 g',
      barcode: null,
      type: 'FINISHED',
      rangeId: RANGE_ID,
      unit: 'PIECE',
      cost: null,
      price: 12500,
      threshold: 30,
      supplierId: null,
      version: null,
    });
    request.flush({ ...article, version: 0 }, { status: 201, statusText: 'Created' });
    await h.fixture.whenStable();
    expect(h.navigate).toHaveBeenCalledWith(['/articles']);
    expect(h.toast.toasts().at(-1)?.messageKey).toBe('articles.form.created');
  });

  it('should stay on the form with empty fields after "save and create another"', async () => {
    await fillValidFinishedProduct();

    await h.press('Enregistrer et créer un autre');
    h.httpMock.expectOne(ARTICLES_URL).flush(article, { status: 201, statusText: 'Created' });
    await h.fixture.whenStable();
    h.fixture.detectChanges();

    expect(h.navigate).not.toHaveBeenCalled();
    expect(h.field<HTMLInputElement>('article-name').value).toBe('');
    expect(h.toast.toasts().at(-1)?.messageKey).toBe('articles.form.created');
  });

  it('should keep a creation made offline in the queue, say so and go back to the list', async () => {
    await fillValidFinishedProduct();
    online.set(false);

    await h.save();

    h.httpMock.expectNone(ARTICLES_URL);
    expect(h.toast.toasts().at(-1)?.messageKey).toBe('common.queuedSaved');
    expect(h.navigate).toHaveBeenCalledWith(['/articles']);
  });

  it('should send the creation with an idempotency key when online', async () => {
    await fillValidFinishedProduct();

    await h.save();

    const request = h.httpMock.expectOne(ARTICLES_URL);
    expect(request.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
    request.flush(article, { status: 201, statusText: 'Created' });
  });

  it('should send nothing when a finished product has no range or no price', async () => {
    await h.type('article-name', 'Gommage café 200 g');

    await h.save();

    h.httpMock.expectNone(ARTICLES_URL);
    expect(h.element.querySelector('#article-price-error')).not.toBeNull();
    expect(h.element.textContent).toContain('Choisissez une gamme');
  });

  it('should create a raw material without range or price', async () => {
    await h.type('article-name', 'Beurre de karité brut');
    await h.choose('type', 'Matière première');
    await h.type('article-unit', 'KILOGRAM');

    await h.save();

    expect(h.element.querySelector('#article-price')).toBeNull();
    expect(h.httpMock.expectOne(ARTICLES_URL).request.body).toMatchObject({
      type: 'RAW_MATERIAL',
      unit: 'KILOGRAM',
      rangeId: null,
      price: null,
    });
  });

  it('should offer the semi-finished type and send it as such', async () => {
    await h.type('article-name', 'Pâte de savon');
    await h.choose('type', 'Semi-fini');

    await h.save();

    expect(h.httpMock.expectOne(ARTICLES_URL).request.body).toMatchObject({
      type: 'SEMI_FINISHED',
      rangeId: null,
      price: null,
    });
  });

  it('should send the supplier chosen', async () => {
    await fillValidFinishedProduct();
    await h.pick('article-supplier', 'Huiles Ravo');

    await h.save();

    expect(h.httpMock.expectOne(ARTICLES_URL).request.body).toMatchObject({
      supplierId: SUPPLIER_ID,
    });
  });

  it('should show the server message and keep the typing when the barcode is already used', async () => {
    await fillValidFinishedProduct();
    await h.type('article-barcode', '3760123456784');
    await h.save();

    h.httpMock
      .expectOne(ARTICLES_URL)
      .flush(problem(409, 'CONFLICT', 'Un article avec ce code-barres existe déjà.'), {
        status: 409,
        statusText: 'Conflict',
      });
    await h.fixture.whenStable();
    h.fixture.detectChanges();

    expect(h.element.querySelector('[role="alert"]')?.textContent).toContain('code-barres');
    expect(h.field<HTMLInputElement>('article-name').value).toBe('  Gommage café 200 g ');
    expect(h.navigate).not.toHaveBeenCalled();
  });

  it('should disable the save buttons while saving so that a double click sends one request', async () => {
    await fillValidFinishedProduct();

    await h.save();
    await h.save();

    expect(h.submit().disabled).toBe(true);
    h.httpMock.expectOne(ARTICLES_URL).flush(article);
    await h.fixture.whenStable();
    h.fixture.detectChanges();
    expect(h.submit().disabled).toBe(false);
  });

  it('should hide the range and the price for a raw material', async () => {
    await h.choose('type', 'Matière première');

    expect(h.element.querySelector('#article-price')).toBeNull();
    expect(h.element.querySelector('input[formControlName="rangeId"]')).toBeNull();
  });
});
