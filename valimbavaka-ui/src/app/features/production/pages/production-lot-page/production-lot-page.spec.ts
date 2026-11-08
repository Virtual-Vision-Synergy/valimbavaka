import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import axe from 'axe-core';
import { of, throwError } from 'rxjs';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { LotCostRDto } from '../../data-access/production-models';
import { ProductionService } from '../../data-access/production-service';
import { ProductionLotPage } from './production-lot-page';

/** 30 savons pour 2 kg d'huile (2 400 Ar) et 0,5 kg de base (6 000 Ar) : 280 Ar l'unité, vendus 8 500 Ar. */
const LOT: LotCostRDto = {
  id: 'b1',
  number: 'LOT-00003',
  articleName: 'Savon noir 100 g',
  quantity: 30,
  status: 'VALIDATED',
  completedOn: '2026-11-08',
  estimated: false,
  lines: [
    {
      articleId: 'mp-1',
      code: 'ART-0008',
      name: 'Huile de coco',
      unit: 'kg',
      quantity: 2,
      unitCost: 1200,
      lineCost: 2400,
    },
    {
      articleId: 'mp-2',
      code: 'ART-0009',
      name: 'Base savonneuse',
      unit: 'kg',
      quantity: 0.5,
      unitCost: 12000,
      lineCost: 6000,
    },
  ],
  materialCost: 8400,
  unitCost: 280,
  uncosted: 0,
  salePrice: 8500,
  margin: 97,
};

const lot = (overrides: Partial<LotCostRDto>): LotCostRDto => ({ ...LOT, ...overrides });

/** PRD-05 : le détail d'un lot affiche ses entrants et le coût de revient unitaire calculé à partir d'eux. */
describe('ProductionLotPage', () => {
  const open = async (cost: ProductionService['cost']): Promise<HTMLElement> => {
    await TestBed.configureTestingModule({
      imports: [ProductionLotPage],
      providers: [
        provideRouter([]),
        provideTestTranslations(),
        { provide: ProductionService, useValue: { cost } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(ProductionLotPage);
    fixture.componentRef.setInput('id', 'b1');
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  };

  const text = (element: Element | null): string =>
    element?.textContent?.replace(/\s+/g, ' ').trim() ?? '';

  const cost = (element: HTMLElement, label: string): string =>
    text(
      Array.from(element.querySelectorAll('.costs > div'))
        .find((item) => text(item.querySelector('dt')) === label)
        ?.querySelector('dd') ?? null,
    );

  it('affiche les entrants et le coût de revient unitaire calculé à partir des entrants', async () => {
    const element = await open(() => of(LOT));

    expect(text(element.querySelector('h1'))).toContain('Lot LOT-00003');
    const rows = Array.from(element.querySelectorAll('tbody tr')).map((row) => text(row));
    expect(rows[0]).toContain('Huile de coco');
    expect(rows[0]).toContain('2 kg');
    expect(rows[0]).toContain('1 200 Ar / kg');
    expect(rows[0]).toContain('2 400 Ar');
    expect(rows[1]).toContain('6 000 Ar');
    expect(cost(element, 'Coût matières')).toBe('8 400 Ar');
    expect(cost(element, 'Coût de revient / u.')).toBe('280 Ar');
    expect(cost(element, 'Prix de vente')).toBe('8 500 Ar');
    expect(cost(element, 'Marge sur prix de vente')).toBe('97 %');
    expect(text(element.querySelector('.alert'))).toContain('Coûts figés à la validation');
  });

  it('signale un lot non terminé : coût estimé avec les prix d’achat actuels', async () => {
    const element = await open(() =>
      of(lot({ status: 'DRAFT', completedOn: null, estimated: true })),
    );

    expect(text(element.querySelector('.alert-warning'))).toContain('coût estimé');
    expect(text(element.querySelector('.sub'))).not.toContain('terminé le');
  });

  it('affiche « — » et un avertissement quand un entrant n’a pas de coût', async () => {
    const element = await open(() =>
      of(
        lot({
          lines: [LOT.lines[0], { ...LOT.lines[1], unitCost: null, lineCost: null }],
          materialCost: null,
          unitCost: null,
          uncosted: 1,
          margin: null,
        }),
      ),
    );

    expect(text(element.querySelectorAll('tbody tr')[1])).toContain('—');
    expect(cost(element, 'Coût de revient / u.')).toBe('—');
    expect(cost(element, 'Marge sur prix de vente')).toBe('—');
    expect(text(element.querySelector('.uncosted'))).toContain("1 entrant(s) sans coût d'achat");
  });

  it('propose de réessayer si le lot ne se charge pas', async () => {
    const element = await open(() => throwError(() => new Error('404')));

    expect(element.querySelector('vvs-list-state')).not.toBeNull();
    expect(element.querySelector('table')).toBeNull();
  });

  it('ne présente aucune violation d’accessibilité détectable', async () => {
    const element = await open(() => of(LOT));
    document.body.appendChild(element);

    const results = await axe.run(element, {
      runOnly: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'],
      rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
    });

    expect(results.violations.map((violation) => violation.id)).toEqual([]);
    element.remove();
  });
});
