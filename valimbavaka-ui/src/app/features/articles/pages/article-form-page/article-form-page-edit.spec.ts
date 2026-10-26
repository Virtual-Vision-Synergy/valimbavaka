import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { FormHarness, mountForm } from './article-form-page.harness';
import {
  ARTICLE_ID,
  ARTICLE_URL,
  RANGE_ID,
  SUPPLIER_ID,
  article,
  problem,
} from './article-form-page.fixtures';

/** Modification d'un article existant avec le formulaire de la maquette (ART-02). */
describe('ArticleFormPage in edit mode', () => {
  let h: FormHarness;

  afterEach(() => {
    try {
      h.httpMock.verify();
    } finally {
      vi.restoreAllMocks();
      TestBed.resetTestingModule();
    }
  });

  const open = async (sheet: object = article): Promise<void> => {
    h = await mountForm(ARTICLE_ID);
    h.httpMock.expectOne(ARTICLE_URL).flush(sheet);
    h.flushChoices();
    await h.fixture.whenStable();
    h.fixture.detectChanges();
  };

  it('should show the code and fill every field with the sheet read from the server', async () => {
    await open();

    expect(h.field<HTMLInputElement>('article-reference').value).toBe('ART-0031');
    expect(h.field<HTMLInputElement>('article-name').value).toBe('Savon noir 100 g');
    expect(h.field<HTMLInputElement>('article-barcode').value).toBe('3760123456784');
    expect(h.field<HTMLInputElement>('article-price').value).toBe('8500');
    expect(h.field<HTMLInputElement>('article-cost').value).toBe('3120.5');
    expect(h.field<HTMLInputElement>('article-threshold').value).toBe('40');
    expect(h.field<HTMLSelectElement>('article-unit').value).toBe('PIECE');
    expect(h.element.querySelector<HTMLInputElement>('input[type="radio"]:checked')).not.toBeNull();
    expect(h.element.textContent).not.toContain('Enregistrer et créer un autre');
  });

  it('should prefill a raw material without range, price, barcode or supplier', async () => {
    await open({
      ...article,
      type: 'RAW_MATERIAL',
      unit: 'KILOGRAM',
      rangeId: null,
      price: null,
      barcode: null,
      supplierId: null,
      cost: 12000,
      threshold: 5,
    });

    expect(h.field<HTMLSelectElement>('article-unit').value).toBe('KILOGRAM');
    expect(h.field<HTMLInputElement>('article-cost').value).toBe('12000');
    expect(h.field<HTMLInputElement>('article-threshold').value).toBe('5');
    expect(h.field<HTMLInputElement>('article-barcode').value).toBe('');
    expect(h.element.querySelector('#article-price')).toBeNull();
  });

  it('should send back exactly the values read when nothing was changed', async () => {
    await open();

    await h.save();

    const request = h.httpMock.expectOne(ARTICLE_URL);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({
      name: 'Savon noir 100 g',
      barcode: '3760123456784',
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

  it('should save with PUT, sending the version read, then go back to the list', async () => {
    await open();
    await h.type('article-name', 'Savon noir 120 g');
    await h.type('article-threshold', '25');

    await h.save();

    const request = h.httpMock.expectOne(ARTICLE_URL);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toMatchObject({
      name: 'Savon noir 120 g',
      threshold: 25,
      version: 3,
    });
    request.flush({ ...article, name: 'Savon noir 120 g', version: 4 });
    await h.fixture.whenStable();
    expect(h.navigate).toHaveBeenCalledWith(['/articles']);
    expect(h.toast.toasts().at(-1)?.messageKey).toBe('articles.form.updated');
  });

  it('should still save a sheet whose stored barcode has a wrong check digit', async () => {
    await open({ ...article, barcode: '3760123456789' });

    await h.save();
    h.httpMock.expectOne(ARTICLE_URL).flush(article);
    await h.fixture.whenStable();
    expect(h.navigate).toHaveBeenCalled();
  });

  it('should send the type change and drop range and price when it becomes a raw material', async () => {
    await open();
    await h.choose('type', 'Matière première');

    await h.save();

    expect(h.element.querySelector('#article-price')).toBeNull();
    expect(h.httpMock.expectOne(ARTICLE_URL).request.body).toMatchObject({
      type: 'RAW_MATERIAL',
      rangeId: null,
      price: null,
    });
  });

  it('should only offer active suppliers, plus the one already linked', async () => {
    await open({ ...article, supplierId: 'inactive' });

    const names = Array.from(h.field<HTMLSelectElement>('article-supplier').options).map((o) =>
      o.textContent?.trim(),
    );

    expect(names).toContain('Huiles Ravo');
    expect(names).toContain('Ancien fournisseur');
  });

  it('should show the server message when the sheet was modified in the meantime', async () => {
    await open();
    await h.save();

    h.httpMock
      .expectOne(ARTICLE_URL)
      .flush(
        problem(
          409,
          'CONFLICT',
          "L'article a été modifié entre-temps : rechargez la fiche puis réessayez.",
        ),
        { status: 409, statusText: 'Conflict' },
      );
    await h.fixture.whenStable();
    h.fixture.detectChanges();

    expect(h.element.textContent).toContain('modifié entre-temps');
    expect(h.navigate).not.toHaveBeenCalled();
    expect(h.field<HTMLInputElement>('article-name').value).toBe('Savon noir 100 g');
  });

  it('should keep the save button disabled until the sheet is read', async () => {
    h = await mountForm(ARTICLE_ID);
    expect(h.submit().disabled).toBe(true);

    h.httpMock.expectOne(ARTICLE_URL).flush(article);
    h.flushChoices();
    await h.fixture.whenStable();
    h.fixture.detectChanges();

    expect(h.submit().disabled).toBe(false);
  });

  it('should show the server message when the sheet does not exist', async () => {
    h = await mountForm(ARTICLE_ID);

    h.httpMock
      .expectOne(ARTICLE_URL)
      .flush(problem(404, 'NOT_FOUND', `Article introuvable : ${ARTICLE_ID}`), {
        status: 404,
        statusText: 'Not Found',
      });
    h.flushChoices();
    await h.fixture.whenStable();
    h.fixture.detectChanges();

    expect(h.element.querySelector('[role="alert"]')?.textContent).toContain('Article introuvable');
  });

  it('should send nothing when the sheet could not be read', async () => {
    h = await mountForm(ARTICLE_ID);
    h.httpMock
      .expectOne(ARTICLE_URL)
      .flush(problem(500, 'INTERNAL_ERROR', 'Erreur interne du serveur.'), {
        status: 500,
        statusText: 'Server Error',
      });
    h.flushChoices();
    await h.fixture.whenStable();
    h.fixture.detectChanges();
    await h.type('article-name', 'Savon écrasé');

    await h.save();

    expect(h.element.querySelector('[role="alert"]')).not.toBeNull();
    expect(h.submit().disabled).toBe(true);
    h.httpMock.expectNone(ARTICLE_URL);
  });
});
