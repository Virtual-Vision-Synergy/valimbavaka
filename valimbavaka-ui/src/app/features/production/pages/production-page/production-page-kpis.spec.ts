import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { PRODUCTION_FIXTURE } from '../../data-access/production-fixtures';
import { ProductionKpisRDto } from '../../data-access/production-models';
import { ProductionService } from '../../data-access/production-service';
import { ProductionPage } from './production-page';

/** PRD-04 : les indicateurs (lots, unités, coût matière) sont affichés en tête de l'écran Production. */
describe('ProductionPage — indicateurs', () => {
  const open = async (kpis: Partial<ProductionKpisRDto>): Promise<HTMLElement> => {
    await TestBed.configureTestingModule({
      imports: [ProductionPage],
      providers: [
        provideRouter([]),
        provideTestTranslations(),
        {
          provide: ProductionService,
          useValue: {
            context: () =>
              of({ ...PRODUCTION_FIXTURE, kpis: { ...PRODUCTION_FIXTURE.kpis, ...kpis } }),
          },
        },
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
    const fixture = TestBed.createComponent(ProductionPage);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  };

  const card = (element: HTMLElement, label: string): HTMLElement =>
    Array.from(element.querySelectorAll<HTMLElement>('.kpis .kpi')).find(
      (item) => item.querySelector('.label')?.textContent?.trim() === label,
    ) as HTMLElement;

  const text = (item: HTMLElement): string => item.textContent?.replace(/\s+/g, ' ').trim() ?? '';

  it('affiche les 3 indicateurs en tête : lots terminés, unités produites, coût matière moyen', async () => {
    const element = await open({
      lotsThisMonth: 18,
      curing: 3,
      unitsProduced: 864,
      unitsTrendPercent: 12,
      averageCost: 3410,
    });

    const labels = Array.from(element.querySelectorAll('.kpis .kpi .label')).map((item) =>
      item.textContent?.trim(),
    );
    expect(labels.slice(0, 3)).toEqual([
      'Lots terminés ce mois',
      'Unités produites',
      'Coût matière moyen',
    ]);
    expect(text(card(element, 'Lots terminés ce mois'))).toContain('18');
    expect(text(card(element, 'Lots terminés ce mois'))).toContain('3 lot(s) en cours');
    expect(text(card(element, 'Unités produites'))).toContain('864');
    expect(text(card(element, 'Unités produites'))).toContain('+12 % vs mois dernier');
    expect(text(card(element, 'Coût matière moyen'))).toContain('3 410');
    expect(text(card(element, 'Coût matière moyen'))).toContain('Ar');
  });

  it('affiche une baisse sans « + » et signale l’absence de production le mois dernier', async () => {
    const down = await open({ unitsTrendPercent: -25 });
    expect(text(card(down, 'Unités produites'))).toContain('-25 % vs mois dernier');

    TestBed.resetTestingModule();
    const none = await open({ unitsTrendPercent: null });
    expect(text(card(none, 'Unités produites'))).toContain('aucune production le mois dernier');
  });

  it('affiche « — » sans unité quand aucun lot n’est chiffré et signale les lots sans coût', async () => {
    const element = await open({ averageCost: null, uncostedLots: 2 });

    const cost = card(element, 'Coût matière moyen');
    expect(cost.querySelector('.num')?.textContent?.trim()).toBe('—');
    expect(cost.querySelector('.unit')).toBeNull();
    expect(text(cost)).toContain("2 lot(s) sans coût d'entrant, non compté(s)");
  });
});
