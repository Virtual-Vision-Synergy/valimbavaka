import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { StockAlertRDto } from '../../data-access/stock-models';
import { ThresholdAlerts } from './threshold-alerts';

const alert = (overrides: Partial<StockAlertRDto>): StockAlertRDto => ({
  articleId: 'art-1',
  code: 'ART-0031',
  name: 'Savon noir 100 g',
  unit: 'PIECE',
  quantity: 12.5,
  threshold: 40,
  shortage: 27.5,
  suggestedQuantity: 30,
  ...overrides,
});

/** STK-04 : liste des articles sous le seuil, telle que calculée par le serveur. */
describe('ThresholdAlerts', () => {
  let fixture: ComponentFixture<ThresholdAlerts>;
  let element: HTMLElement;

  const render = async (alerts: StockAlertRDto[], total = alerts.length): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [ThresholdAlerts],
      providers: [provideTestTranslations()],
    }).compileComponents();
    fixture = TestBed.createComponent(ThresholdAlerts);
    fixture.componentRef.setInput('alerts', alerts);
    fixture.componentRef.setInput('total', total);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    element = fixture.nativeElement as HTMLElement;
  };

  /** Texte de chaque ligne, cellules séparées par « | » (Angular retire les espaces entre balises). */
  const rows = (): string[] =>
    Array.from(element.querySelectorAll('tbody tr')).map((row) =>
      Array.from(row.querySelectorAll('td'))
        .map((cell) => cell.textContent?.trim() ?? '')
        .join(' | '),
    );

  it('affiche stock actuel, seuil, manque et quantité suggérée avec son unité', async () => {
    await render([alert({})]);

    const headers = Array.from(element.querySelectorAll('thead th')).map((th) =>
      th.textContent?.trim(),
    );
    expect(headers).toEqual(['Article', 'Stock actuel', 'Seuil', 'Manque', 'Quantité suggérée']);
    expect(rows()[0]).toContain('12.5 | 40 | 27.5 | 30');
    expect(element.querySelector('tbody .unit')?.textContent?.trim()).toBe('Pièce');
    expect(rows()[0]).toContain('Sous le seuil');
  });

  it('signale une rupture quand le stock est nul', async () => {
    await render([alert({ quantity: 0, threshold: 15, shortage: 15, suggestedQuantity: 20 })]);

    expect(rows()[0]).toContain('En rupture');
    expect(rows()[0]).toContain('0 | 15 | 15 | 20');
  });

  it('garde l’ordre du serveur et indique quand seules les plus urgentes sont affichées', async () => {
    await render(
      [alert({ articleId: 'a', code: 'ART-0002' }), alert({ articleId: 'b', code: 'ART-0001' })],
      140,
    );

    expect(rows().map((row) => row.match(/ART-\d+/)?.[0])).toEqual(['ART-0002', 'ART-0001']);
    expect(element.querySelector('h2')?.textContent).toContain('140 article(s)');
    expect(element.querySelector('.subtitle')?.textContent).toContain('Les 2 plus urgents');
  });
});
