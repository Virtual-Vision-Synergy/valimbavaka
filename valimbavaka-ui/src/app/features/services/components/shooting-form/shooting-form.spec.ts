import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { StockRDto } from '../../../stocks/data-access/stock-models';
import { ShootingWDto } from '../../data-access/service-models';
import { ShootingForm } from './shooting-form';

const article = (articleId: string, name: string, store: number): StockRDto => ({
  articleId,
  code: `ART-${articleId}`,
  name,
  rangeId: null,
  unit: 'PIECE',
  threshold: 0,
  cost: null,
  total: store + 10,
  locations: [
    { locationId: 'loc-warehouse', code: 'WAREHOUSE', label: 'Entrepôt', quantity: 10 },
    { locationId: 'loc-store', code: 'STORE', label: 'Boutique', quantity: store },
  ],
});

/** SHO-01 : saisie d'une sortie shooting (boutique par défaut, pas plus que le disponible). */
describe('ShootingForm', () => {
  let fixture: ComponentFixture<ShootingForm>;
  let element: HTMLElement;
  let emitted: ShootingWDto[];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ShootingForm],
      providers: [provideTestTranslations()],
    }).compileComponents();
    fixture = TestBed.createComponent(ShootingForm);
    fixture.componentRef.setInput('stock', [
      article('0031', 'Gommage café 200 g', 4),
      article('0007', 'Savon noir 100 g', 0),
    ]);
    emitted = [];
    fixture.componentInstance.submitted.subscribe((value) => emitted.push(value));
    fixture.detectChanges();
    await fixture.whenStable();
    element = fixture.nativeElement as HTMLElement;
  });

  const select = (selector: string): HTMLSelectElement =>
    element.querySelector(selector) as HTMLSelectElement;

  const addArticle = (articleId: string): void => {
    const adder = select('.add select');
    adder.value = articleId;
    adder.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };

  const setQuantity = (value: string): void => {
    const input = element.querySelector('.qty') as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };

  const submit = (): void => {
    (element.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  };

  it('propose la boutique comme emplacement de sortie par défaut', () => {
    expect(select('#sho-location').value).toBe('loc-store');
  });

  it('envoie emplacement, articles, quantités et retour prévu au format ISO', () => {
    addArticle('0031');
    setQuantity('3');

    submit();

    expect(emitted).toHaveLength(1);
    expect(emitted[0]?.locationId).toBe('loc-store');
    expect(emitted[0]?.items).toEqual([{ articleId: '0031', quantity: 3 }]);
    expect(emitted[0]?.expectedReturnAt).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/);
    expect(emitted[0]?.note).toBeNull();
  });

  it("refuse une sortie sans article et l'explique", () => {
    submit();

    expect(emitted).toHaveLength(0);
    expect(element.textContent).toContain('Ajoutez au moins un article.');
  });

  it('signale une quantité supérieure au disponible de la boutique et bloque l’envoi', () => {
    addArticle('0031');
    setQuantity('5');

    submit();

    expect(emitted).toHaveLength(0);
    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Stock insuffisant');
  });

  it('refuse un retour prévu déjà passé', () => {
    addArticle('0031');
    const when = element.querySelector('#sho-return') as HTMLInputElement;
    when.value = '2020-01-01T10:00';
    when.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    submit();

    expect(emitted).toHaveLength(0);
    expect(element.textContent).toContain('Le retour prévu doit être dans le futur.');
  });
});
