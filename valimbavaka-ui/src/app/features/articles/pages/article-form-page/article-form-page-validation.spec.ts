import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { FormHarness, mountForm } from './article-form-page.harness';
import { ARTICLES_URL } from './article-form-page.fixtures';

/** Règles de saisie du formulaire : mêmes bornes que le serveur (422 évité côté écran). */
describe('ArticleFormPage validation', () => {
  let h: FormHarness;

  beforeEach(async () => {
    h = await mountForm();
    h.flushChoices();
    await h.fixture.whenStable();
    h.fixture.detectChanges();
    await h.type('article-name', 'Gommage café 200 g');
    await h.choose('rangeId', 'Botanika');
    await h.type('article-price', '12500');
  });

  afterEach(() => {
    try {
      h.httpMock.verify();
    } finally {
      vi.restoreAllMocks();
      TestBed.resetTestingModule();
    }
  });

  it('should send nothing and ask for a name when the name is only spaces', async () => {
    await h.type('article-name', '   ');

    await h.save();

    h.httpMock.expectNone(ARTICLES_URL);
    expect(h.element.querySelector('#article-name-error')?.textContent).toContain('obligatoire');
  });

  it('should send nothing when the name exceeds 120 characters, and accept exactly 120', async () => {
    await h.type('article-name', 'a'.repeat(121));
    await h.save();
    h.httpMock.expectNone(ARTICLES_URL);
    expect(h.element.querySelector('#article-name-error')?.textContent).toContain('120');

    await h.type('article-name', 'a'.repeat(120));
    await h.save();
    expect(h.httpMock.expectOne(ARTICLES_URL).request.body).toMatchObject({
      name: 'a'.repeat(120),
    });
  });

  it.each(['12345', 'abcdefghijklm', '37601234567890'])(
    'should send nothing when the barcode is %s',
    async (barcode) => {
      await h.type('article-barcode', barcode);

      await h.save();

      h.httpMock.expectNone(ARTICLES_URL);
      expect(h.element.querySelector('#article-barcode-error')?.textContent).toContain(
        '13 chiffres',
      );
    },
  );

  it('should refuse a barcode whose check digit is wrong', async () => {
    await h.type('article-barcode', '3760123456789');

    await h.save();

    h.httpMock.expectNone(ARTICLES_URL);
    expect(h.element.querySelector('#article-barcode-error')?.textContent).toContain('Clé');
  });

  it('should send a barcode typed with spaces without the spaces', async () => {
    await h.type('article-barcode', '3760 1234 56784');
    await h.save();
    expect(h.httpMock.expectOne(ARTICLES_URL).request.body).toMatchObject({
      barcode: '3760123456784',
    });
  });

  it('should send an empty barcode as null', async () => {
    await h.type('article-barcode', '');

    await h.save();

    expect(h.httpMock.expectOne(ARTICLES_URL).request.body).toMatchObject({ barcode: null });
  });

  it('should refuse a negative cost, price or threshold and accept zero', async () => {
    await h.type('article-cost', '-0.01');
    await h.type('article-price', '-1');
    await h.type('article-threshold', '-5');
    await h.save();
    h.httpMock.expectNone(ARTICLES_URL);
    expect(h.element.querySelector('#article-cost-error')).not.toBeNull();
    expect(h.element.querySelector('#article-price-error')).not.toBeNull();
    expect(h.element.querySelector('#article-threshold-error')).not.toBeNull();

    await h.type('article-cost', '0');
    await h.type('article-price', '0');
    await h.type('article-threshold', '0');
    await h.save();
    expect(h.httpMock.expectOne(ARTICLES_URL).request.body).toMatchObject({
      cost: 0,
      price: 0,
      threshold: 0,
    });
  });
});
