import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { StockAlertRDto } from '../../data-access/stock-models';
import { RestockDraft } from './restock-draft';

const alert = (overrides: Partial<StockAlertRDto>): StockAlertRDto => ({
  articleId: 'art-1',
  code: 'ART-0031',
  name: 'Savon noir 100 g',
  unit: 'PIECE',
  quantity: 12.5,
  threshold: 40,
  shortage: 27.5,
  suggestedQuantity: 30,
  supplierId: 'sup-1',
  supplierName: 'Huiles Ravo',
  leadTimeDays: 7,
  expectedDeliveryDate: '2026-11-15',
  ...overrides,
});

/** STK-05 : brouillon du bon, construit à partir des alertes de seuil du serveur (STK-04). */
describe('RestockDraft', () => {
  let fixture: ComponentFixture<RestockDraft>;
  let element: HTMLElement;

  const render = async (alerts: StockAlertRDto[], busy = false): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [RestockDraft],
      providers: [provideTestTranslations()],
    }).compileComponents();
    fixture = TestBed.createComponent(RestockDraft);
    fixture.componentRef.setInput('alerts', alerts);
    fixture.componentRef.setInput('busy', busy);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    element = fixture.nativeElement as HTMLElement;
  };

  const text = (selector: string): string[] =>
    Array.from(element.querySelectorAll(selector)).map(
      (node) => node.textContent?.replace(/\s+/g, ' ').trim() ?? '',
    );

  const validateButton = (): HTMLButtonElement =>
    element.querySelectorAll<HTMLButtonElement>('.actions button')[1];

  it('regroupe les articles par fournisseur avec la quantité suggérée et la livraison prévue', async () => {
    await render([
      alert({}),
      alert({ articleId: 'art-2', name: 'Huile de coco', suggestedQuantity: 20 }),
      alert({
        articleId: 'art-3',
        name: 'Baume karité',
        suggestedQuantity: 10,
        supplierId: null,
        supplierName: null,
        leadTimeDays: null,
        expectedDeliveryDate: null,
      }),
    ]);

    expect(text('.group-title')).toEqual([
      'Huiles Ravo · délai 7 j',
      'Fabrication interne (atelier)',
    ]);
    expect(text('.lines li')).toEqual([
      'Savon noir 100 g 30 u.',
      'Huile de coco 20 u.',
      'Baume karité 10 u.',
    ]);
    expect(text('.hint')).toEqual(["Livraison prévue le 15 nov. si la commande part aujourd'hui."]);
    expect(text('.subtitle')[0]).toContain('3 article(s) sous le seuil');
  });

  it('émet la validation et la bloque pendant la génération', async () => {
    await render([alert({})]);
    let validated = 0;
    fixture.componentInstance.validated.subscribe(() => validated++);

    validateButton().click();
    expect(validated).toBe(1);

    fixture.componentRef.setInput('busy', true);
    fixture.detectChanges();
    expect(validateButton().disabled).toBe(true);
  });
});
