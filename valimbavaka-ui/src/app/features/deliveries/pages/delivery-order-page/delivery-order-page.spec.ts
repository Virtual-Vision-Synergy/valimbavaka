import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { type MockInstance, vi } from 'vitest';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { ApiError } from '../../../../core/http/problem-detail';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { ArticleRDto } from '../../../article/data-access/article-models';
import { ArticleService } from '../../../article/data-access/article-service';
import { businessToday } from '../../data-access/delivery-mapping';
import { DeliveryOrderRDto } from '../../data-access/delivery-models';
import { DeliveryService } from '../../data-access/delivery-service';
import { DeliveryOrderPage } from './delivery-order-page';

const SOAP: ArticleRDto = {
  id: 'soap',
  code: 'ART-0031',
  name: 'Savon noir 100 g',
  barcode: null,
  type: 'FINISHED',
  rangeId: null,
  unit: 'PIECE',
  cost: null,
  price: 8500,
  threshold: 10,
  supplierId: null,
  active: true,
  createdAt: '2026-11-08T08:00:00Z',
  updatedAt: '2026-11-08T08:00:00Z',
  version: 0,
};

const CREATED = { id: 'order-id', number: 'LIV-0042', stopNumber: 3 } as DeliveryOrderRDto;

describe('DeliveryOrderPage', () => {
  const create = vi.fn();
  const list = vi.fn();
  let fixture: ComponentFixture<DeliveryOrderPage>;
  let element: HTMLElement;
  let navigate: MockInstance<Router['navigate']>;
  let toast: ToastService;

  const open = async (): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [DeliveryOrderPage],
      providers: [
        provideRouter([]),
        provideTestTranslations(),
        {
          provide: DeliveryService,
          useValue: {
            create,
            drivers: () => of([{ id: 'tojo', firstName: 'Tojo', lastName: 'Nirina' }]),
          },
        },
        { provide: ArticleService, useValue: { list } },
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('SELLER'));
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    toast = TestBed.inject(ToastService);
    fixture = TestBed.createComponent(DeliveryOrderPage);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const type = (selector: string, value: string): void => {
    const field = element.querySelector<HTMLInputElement | HTMLSelectElement>(selector);
    if (!field) {
      throw new Error(`Champ introuvable : ${selector}`);
    }
    field.value = value;
    field.dispatchEvent(new Event(field instanceof HTMLSelectElement ? 'change' : 'input'));
  };

  const fill = async (quantity = '2'): Promise<void> => {
    type('#customerName', '  Rakoto Be ');
    type('#customerPhone', '034 12 345 67');
    type('#address', 'Lot II J 45, rue Pasteur');
    type('#driverId', 'tojo');
    type('#article-0', 'soap');
    type('#quantity-0', quantity);
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const submit = async (): Promise<void> => {
    element.querySelector<HTMLFormElement>('form')?.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    await fixture.whenStable();
  };

  beforeEach(() => {
    create.mockReset();
    list
      .mockReset()
      .mockReturnValue(of({ items: [SOAP], page: 1, pageSize: 100, totalCount: 1, totalPages: 1 }));
  });

  it('propose les livreurs actifs et seulement les produits finis actifs, livraison aujourd’hui', async () => {
    await open();

    expect(list).toHaveBeenCalledWith(expect.objectContaining({ type: 'FINISHED', active: true }));
    expect(element.querySelector('#driverId')?.textContent).toContain('Tojo N.');
    expect(element.querySelector('#article-0')?.textContent).toContain(
      'ART-0031 · Savon noir 100 g',
    );
    expect(element.querySelector<HTMLInputElement>('#scheduledDate')?.value).toBe(businessToday());
  });

  it('affiche le montant estimé à encaisser selon les prix des articles', async () => {
    await open();
    await fill('2');

    expect(element.querySelector('[data-testid="estimate"]')?.textContent).toContain('17');
  });

  it('crée la commande, annonce son arrêt dans la tournée et revient au tableau', async () => {
    create.mockReturnValue(of({ queued: false, body: CREATED }));
    await open();
    const show = vi.spyOn(toast, 'show');
    await fill('2,5');

    await submit();

    expect(create).toHaveBeenCalledWith({
      customerName: 'Rakoto Be',
      customerPhone: '034 12 345 67',
      address: 'Lot II J 45, rue Pasteur',
      district: null,
      driverId: 'tojo',
      scheduledDate: businessToday(),
      lines: [{ articleId: 'soap', quantity: 2.5 }],
      note: null,
    });
    expect(show).toHaveBeenCalledWith('deliveries.form.saved', { number: 'LIV-0042', stop: 3 });
    expect(navigate).toHaveBeenCalledWith(['/deliveries']);
  });

  it('annonce une commande mise en file hors connexion', async () => {
    create.mockReturnValue(of({ queued: true }));
    await open();
    const show = vi.spyOn(toast, 'show');
    await fill();

    await submit();

    expect(show).toHaveBeenCalledWith('common.queuedSaved', {}, 'info');
  });

  it('refuse un formulaire sans livreur, ou avec un téléphone ou un jour passé invalides', async () => {
    await open();
    await fill();
    type('#driverId', '');
    type('#customerPhone', 'appelez-moi');
    type('#scheduledDate', '2020-01-01');
    fixture.detectChanges();

    await submit();

    expect(create).not.toHaveBeenCalled();
    expect(element.querySelector('#driverId-error')?.textContent).toContain(
      'Choisissez le livreur',
    );
    expect(element.querySelector('#customerPhone-error')).not.toBeNull();
    expect(element.querySelector('#scheduledDate-error')?.textContent).toContain('dans le passé');
  });

  it('affiche le refus du serveur et garde la saisie', async () => {
    const refusal = "Le livreur choisi est inconnu, désactivé ou n'a pas le rôle Livreur.";
    create.mockReturnValue(throwError(() => new ApiError(422, 'VALIDATION_FAILED', refusal)));
    await open();
    await fill();

    await submit();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(refusal);
    expect(element.querySelector<HTMLInputElement>('#customerName')?.value).toBe('  Rakoto Be ');
    expect(navigate).not.toHaveBeenCalled();
  });
});
