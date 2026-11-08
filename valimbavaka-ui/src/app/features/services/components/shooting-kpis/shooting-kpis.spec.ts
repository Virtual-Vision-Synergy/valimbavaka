import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ShootingSummaryRDto } from '../../data-access/service-models';
import { ShootingKpis } from './shooting-kpis';

const summary = (overrides: Partial<ShootingSummaryRDto> = {}): ShootingSummaryRDto => ({
  outCount: 3,
  lateCount: 1,
  immobilizedValue: 12241.5,
  unvaluedQuantity: 0,
  ...overrides,
});

/** SHO-03 : les 3 indicateurs en tête de l'onglet Shooting. */
describe('ShootingKpis', () => {
  let fixture: ComponentFixture<ShootingKpis>;
  let element: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ShootingKpis],
      providers: [provideTestTranslations()],
    }).compileComponents();
    fixture = TestBed.createComponent(ShootingKpis);
    element = fixture.nativeElement as HTMLElement;
  });

  const cards = (): string[] =>
    Array.from(element.querySelectorAll('vvs-kpi-card')).map((card) =>
      (card.textContent ?? '').replace(/\s+/g, ' ').trim(),
    );

  it('affiche les sorties en cours, les retards et la valeur immobilisée en ariary', () => {
    fixture.componentRef.setInput('summary', summary());
    fixture.detectChanges();

    const [out, late, value] = cards();
    expect(cards()).toHaveLength(3);
    expect(out).toContain('Sorties en cours');
    expect(out).toContain('3');
    expect(late).toContain('En retard');
    expect(late).toContain('1');
    expect(late).toContain('À relancer');
    expect(value).toContain('Valeur immobilisée');
    expect(value).toMatch(/12\s242\s?Ar/);
  });

  it("n'affiche pas de badge sans retard et signale les unités sans coût", () => {
    fixture.componentRef.setInput('summary', summary({ lateCount: 0, unvaluedQuantity: 2.5 }));
    fixture.detectChanges();

    const [, late, value] = cards();
    expect(late).not.toContain('À relancer');
    expect(value).toContain('dont 2.5 unité(s) sans coût');
  });

  it('affiche des zéros quand aucune sortie n’est dehors', () => {
    fixture.componentRef.setInput(
      'summary',
      summary({ outCount: 0, lateCount: 0, immobilizedValue: 0, unvaluedQuantity: 0 }),
    );
    fixture.detectChanges();

    const [out, late, value] = cards();
    expect(out).toContain('0');
    expect(late).toContain('0');
    expect(value).toMatch(/0\s?Ar/);
    expect(value).not.toContain('sans coût');
  });
});
