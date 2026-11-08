import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { SalonProductRDto, TreatmentRDto } from '../../data-access/service-models';
import { ServiceRecordWDto } from '../../data-access/service-record-models';
import { SalonForm } from './salon-form';

const PRODUCTS: readonly SalonProductRDto[] = [
  { id: 'huile-coco', name: 'Huile de coco vierge', unit: 'L', stock: 0.2 },
  { id: 'masque', name: 'Masque argile rose', unit: 'u.', stock: 3 },
];

const TREATMENTS: readonly TreatmentRDto[] = [
  {
    id: 'soin-visage',
    name: 'Soin visage éclat',
    minutes: 45,
    price: 35000,
    recipe: [
      { productId: 'huile-coco', quantity: 0.015 },
      { productId: 'masque', quantity: 1 },
    ],
  },
];

/** Enregistrement d'une prestation (SRV-02) : ce que la vendeuse voit et ce qui part au serveur. */
describe('SalonForm', () => {
  let fixture: ComponentFixture<SalonForm>;
  let element: HTMLElement;
  let emitted: ServiceRecordWDto[];

  const render = async (treatments: readonly TreatmentRDto[] = TREATMENTS): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [SalonForm],
      providers: [provideTestTranslations()],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('SELLER'));
    fixture = TestBed.createComponent(SalonForm);
    fixture.componentRef.setInput('treatments', treatments);
    fixture.componentRef.setInput('products', PRODUCTS);
    emitted = [];
    fixture.componentInstance.registered.subscribe((request) => emitted.push(request));
    await fixture.whenStable();
    element = fixture.nativeElement as HTMLElement;
  };

  const submit = async (): Promise<void> => {
    element.querySelector<HTMLFormElement>('form')?.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  };

  it('affiche la prestation au nom du compte connecté', async () => {
    await render();

    expect(element.querySelector('.seller')?.textContent).toContain('Hanta Rasolofoson');
  });

  it('envoie le premier soin avec la recette pré-remplie et sans cliente', async () => {
    await render();

    await submit();

    expect(emitted).toEqual([
      {
        salonServiceId: 'soin-visage',
        customerName: null,
        products: [
          { articleId: 'huile-coco', quantity: 0.015 },
          { articleId: 'masque', quantity: 1 },
        ],
      },
    ]);
  });

  it("n'envoie pas un produit ramené à zéro et arrondit la quantité à 3 décimales", async () => {
    await render();
    const quantities = element.querySelectorAll<HTMLInputElement>('input.qty');
    quantities[0].value = '0,0204';
    quantities[0].dispatchEvent(new Event('change'));
    quantities[1].value = '0';
    quantities[1].dispatchEvent(new Event('change'));
    await fixture.whenStable();

    await submit();

    expect(emitted[0].products).toEqual([{ articleId: 'huile-coco', quantity: 0.02 }]);
  });

  it("bloque l'envoi quand le stock salon est insuffisant", async () => {
    await render();
    const quantity = element.querySelector<HTMLInputElement>('input.qty');
    if (quantity) {
      quantity.value = '0.5';
      quantity.dispatchEvent(new Event('change'));
    }
    await fixture.whenStable();

    await submit();

    expect(emitted).toEqual([]);
    expect(element.querySelector('[role="alert"]')).not.toBeNull();
  });

  it("n'envoie rien quand le catalogue n'a aucun service actif", async () => {
    await render([]);

    await submit();

    expect(emitted).toEqual([]);
    expect(element.textContent).toContain('Aucun service actif');
  });
});
