import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ServiceRecordRowRDto, ServicesContextRDto } from '../../data-access/service-models';
import { SalonService } from '../../data-access/service-service';
import { ShootingService } from '../../data-access/shooting-service';
import { ServicesPage } from './services-page';

const FACIAL: ServiceRecordRowRDto = {
  id: 'p2',
  number: 'PRE-0002',
  treatmentName: 'Soin visage éclat',
  time: '10:22',
  customer: 'Hanta',
  products: [
    { name: 'Huile de coco vierge', quantity: 0.015, unit: 'L' },
    { name: 'Masque argile rose', quantity: 1, unit: 'u.' },
  ],
  price: 35000,
};

const MANICURE: ServiceRecordRowRDto = {
  id: 'p1',
  number: 'PRE-0001',
  treatmentName: 'Manucure',
  time: '09:05',
  customer: null,
  products: [],
  price: 15000,
};

const contextWith = (today: readonly ServiceRecordRowRDto[]): ServicesContextRDto => ({
  treatments: [],
  products: [],
  today,
});

/** Historique des prestations du jour (SRV-03) : heure, service, cliente et produits consommés de chaque prestation. */
describe('ServicesPage', () => {
  let fixture: ComponentFixture<ServicesPage>;
  let element: HTMLElement;

  const render = async (today: readonly ServiceRecordRowRDto[]): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [ServicesPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTestTranslations(),
        {
          provide: SalonService,
          useValue: { context: () => of(contextWith(today)) },
        },
        // Sorties shooting (SHO-01) hors sujet ici : une liste vide évite une requête HTTP en attente.
        { provide: ShootingService, useValue: { list: () => of([]) } },
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('SELLER'));
    fixture = TestBed.createComponent(ServicesPage);
    await fixture.whenStable();
    element = fixture.nativeElement as HTMLElement;
  };

  const rows = (): string[] =>
    Array.from(element.querySelectorAll('.today li')).map((row) =>
      (row.textContent ?? '').replace(/\s+/g, ' ').trim(),
    );

  it("affiche l'heure, le service, la cliente et les produits consommés de chaque prestation", async () => {
    await render([FACIAL, MANICURE]);

    const [facial, manicure] = rows();
    expect(element.querySelector('#today-title')?.textContent).toContain('Mes prestations du jour');
    expect(facial).toContain('Soin visage éclat');
    expect(facial).toContain('10:22 · Hanta');
    expect(facial).toContain('Huile de coco vierge · 0.015 L');
    expect(facial).toContain('Masque argile rose · 1 u.');
    expect(manicure).toContain('09:05 · Cliente non renseignée');
    expect(manicure).toContain('Aucun produit consommé');
  });

  it("annonce qu'aucune prestation n'a encore été enregistrée aujourd'hui", async () => {
    await render([]);

    expect(rows()).toEqual(["Aucune prestation enregistrée aujourd'hui."]);
  });
});
