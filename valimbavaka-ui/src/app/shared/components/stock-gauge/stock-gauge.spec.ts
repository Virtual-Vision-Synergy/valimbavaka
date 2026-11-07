import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTestTranslations } from '../../../core/i18n/i18n-testing';
import { StockGauge, StockGaugeSegment } from './stock-gauge';

const SEGMENTS: readonly StockGaugeSegment[] = [
  { key: 'WAREHOUSE', labelKey: 'stocks.location.WAREHOUSE', quantity: 30 },
  { key: 'SHOP', labelKey: 'stocks.location.SHOP', quantity: 10 },
  { key: 'DELIVERY', labelKey: 'stocks.location.DELIVERY', quantity: 0 },
  { key: 'SALON', labelKey: 'stocks.location.SALON', quantity: 0 },
];

/** STK-02 : jauge de répartition du stock par emplacement, détail chiffré au survol. */
describe('StockGauge', () => {
  let fixture: ComponentFixture<StockGauge>;
  let element: HTMLElement;

  const render = async (
    segments: readonly StockGaugeSegment[],
    value = 40,
    threshold = 20,
  ): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [StockGauge],
      providers: [provideTestTranslations()],
    }).compileComponents();
    fixture = TestBed.createComponent(StockGauge);
    fixture.componentRef.setInput('value', value);
    fixture.componentRef.setInput('threshold', threshold);
    fixture.componentRef.setInput('max', 50);
    fixture.componentRef.setInput('segments', segments);
    element = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  };

  const track = (): HTMLElement => element.querySelector('.track') as HTMLElement;
  const tip = (): HTMLElement => element.querySelector('[role="tooltip"]') as HTMLElement;
  const rows = (): string[] =>
    Array.from(tip().querySelectorAll('.tip-row')).map((row) =>
      Array.from(row.children)
        .map((cell) => cell.textContent?.trim() ?? '')
        .filter((text) => text !== '')
        .join(' '),
    );

  it('reste décorative et sans bulle quand aucune répartition n’est fournie', async () => {
    await render([]);

    expect(element.getAttribute('aria-hidden')).toBe('true');
    expect(tip()).toBeNull();
    expect(track().getAttribute('tabindex')).toBeNull();
  });

  it('dessine une part par emplacement, proportionnelle à la pleine échelle', async () => {
    await render(SEGMENTS);

    const widths = Array.from(element.querySelectorAll<HTMLElement>('.segment')).map(
      (segment) => segment.style.width,
    );
    expect(widths).toEqual(['60%', '20%', '0%', '0%']);
  });

  it('affiche au survol le détail chiffré de chaque emplacement et le total', async () => {
    await render(SEGMENTS);

    track().dispatchEvent(new MouseEvent('mouseenter'));
    await fixture.whenStable();

    expect(tip().classList).toContain('open');
    expect(rows()).toEqual([
      'Entrepôt 30 75 %',
      'Boutique 10 25 %',
      'Livraison 0 0 %',
      'Salon 0 0 %',
      'Total 40',
    ]);
  });

  it('referme la bulle quand le pointeur quitte la jauge', async () => {
    await render(SEGMENTS);
    track().dispatchEvent(new MouseEvent('mouseenter'));
    await fixture.whenStable();

    track().dispatchEvent(new MouseEvent('mouseleave'));
    await fixture.whenStable();

    expect(tip().classList).not.toContain('open');
  });

  it('ouvre la bulle au focus clavier et la ferme avec Échap', async () => {
    await render(SEGMENTS);

    track().dispatchEvent(new FocusEvent('focus'));
    await fixture.whenStable();
    expect(tip().classList).toContain('open');

    track().dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    await fixture.whenStable();
    expect(tip().classList).not.toContain('open');
  });

  it('est atteignable au clavier et décrite par le détail pour les lecteurs d’écran', async () => {
    await render(SEGMENTS);

    expect(element.getAttribute('aria-hidden')).toBeNull();
    expect(track().getAttribute('tabindex')).toBe('0');
    expect(track().getAttribute('aria-label')).toBe('Répartition du stock par emplacement');
    expect(track().getAttribute('aria-describedby')).toBe(tip().id);
  });

  it('réduit les parts pour tenir dans la jauge quand le stock dépasse la pleine échelle', async () => {
    await render(
      [
        { key: 'WAREHOUSE', labelKey: 'stocks.location.WAREHOUSE', quantity: 150 },
        { key: 'SHOP', labelKey: 'stocks.location.SHOP', quantity: 50 },
      ],
      200,
    );

    const widths = Array.from(element.querySelectorAll<HTMLElement>('.segment')).map(
      (segment) => segment.style.width,
    );
    expect(widths).toEqual(['75%', '25%']);
  });

  it('affiche 0 % partout et signale la rupture quand le stock est vide', async () => {
    await render(
      SEGMENTS.map((segment) => ({ ...segment, quantity: 0 })),
      0,
    );
    track().dispatchEvent(new MouseEvent('mouseenter'));
    await fixture.whenStable();

    expect(track().getAttribute('data-level')).toBe('out');
    expect(rows()[0]).toBe('Entrepôt 0 0 %');
    expect(rows()[4]).toBe('Total 0');
  });
});
