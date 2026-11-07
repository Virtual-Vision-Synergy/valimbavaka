import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { type MockInstance, vi } from 'vitest';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { ApiError } from '../../../../core/http/problem-detail';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { ReferentialService } from '../../../referential/data-access/referential-service';
import { StockRDto } from '../../../stocks/data-access/stock-models';
import { StockService } from '../../../stocks/data-access/stock-service';
import { TransferRDto } from '../../data-access/transfer-models';
import { TransferService } from '../../data-access/transfer-service';
import { StockReturnPage } from './stock-return-page';

const LOCATIONS = [
  { id: 'warehouse-id', code: 'WAREHOUSE', label: 'Entrepôt' },
  { id: 'store-id', code: 'STORE', label: 'Boutique' },
];

function stock(articleId: string, name: string, store: number): StockRDto {
  return {
    articleId,
    code: `ART-00${articleId.length}`,
    name,
    rangeId: null,
    unit: 'PIECE',
    threshold: 10,
    cost: null,
    total: store,
    locations: [
      { locationId: 'warehouse-id', code: 'WAREHOUSE', label: 'Entrepôt', quantity: 0 },
      { locationId: 'store-id', code: 'STORE', label: 'Boutique', quantity: store },
    ],
  };
}

const RECORDED: TransferRDto = {
  id: 'transfer-id',
  number: 'RET-0016',
  origin: 'RETURN',
  status: 'RETURN',
  fromLocationId: 'store-id',
  toLocationId: 'warehouse-id',
  note: null,
  lines: [{ articleId: 'soap', quantity: 2.5 }],
  createdAt: '2026-11-08T16:30:00Z',
  createdBy: null,
};

describe('StockReturnPage', () => {
  const recordReturn = vi.fn();
  let fixture: ComponentFixture<StockReturnPage>;
  let element: HTMLElement;
  let navigate: MockInstance<Router['navigate']>;
  let toast: ToastService;

  const open = async (role: 'ADMIN' | 'SELLER' = 'SELLER'): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [StockReturnPage],
      providers: [
        provideRouter([]),
        provideTestTranslations(),
        { provide: TransferService, useValue: { recordReturn } },
        { provide: ReferentialService, useValue: { listStockLocations: () => of(LOCATIONS) } },
        {
          provide: StockService,
          useValue: {
            quantities: () =>
              of([stock('soap', 'Savon noir', 12), stock('oil', 'Huile de coco', 0)]),
          },
        },
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser(role));
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    toast = TestBed.inject(ToastService);
    fixture = TestBed.createComponent(StockReturnPage);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const select = (selector: string): HTMLSelectElement =>
    element.querySelector<HTMLSelectElement>(selector) as HTMLSelectElement;

  const fillLine = async (articleId: string, quantity: string): Promise<void> => {
    const article = select('#article-0');
    article.value = articleId;
    article.dispatchEvent(new Event('change'));
    const input = element.querySelector<HTMLInputElement>('#quantity-0') as HTMLInputElement;
    input.value = quantity;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const submit = async (): Promise<void> => {
    element.querySelector<HTMLFormElement>('form')?.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    await fixture.whenStable();
  };

  beforeEach(() => recordReturn.mockReset());

  it('propose par défaut un retour de la boutique vers l’entrepôt', async () => {
    await open();

    expect(select('#fromLocationId').value).toBe('store-id');
    expect(select('#toLocationId').value).toBe('warehouse-id');
  });

  it('ne propose que les articles en stock à l’origine, avec la quantité disponible', async () => {
    await open();

    const options = Array.from(select('#article-0').options).map((option) =>
      option.textContent?.trim(),
    );
    expect(options).toHaveLength(2);
    expect(options[1]).toContain('Savon noir (12)');
    expect(options.join(' ')).not.toContain('Huile de coco');
  });

  it('envoie le retour, annonce son numéro et revient aux stocks pour la vendeuse', async () => {
    recordReturn.mockReturnValue(of({ queued: false, body: RECORDED }));
    await open('SELLER');
    const show = vi.spyOn(toast, 'show');
    await fillLine('soap', '2,5');

    await submit();

    expect(recordReturn).toHaveBeenCalledWith({
      fromLocationId: 'store-id',
      toLocationId: 'warehouse-id',
      lines: [{ articleId: 'soap', quantity: 2.5 }],
      note: null,
    });
    expect(show).toHaveBeenCalledWith('transfers.return.saved', { number: 'RET-0016' });
    expect(navigate).toHaveBeenCalledWith(['/stocks']);
  });

  it('revient à la liste des transferts pour l’administrateur', async () => {
    recordReturn.mockReturnValue(of({ queued: false, body: RECORDED }));
    await open('ADMIN');
    await fillLine('soap', '1');

    await submit();

    expect(navigate).toHaveBeenCalledWith(['/transfers']);
  });

  it('annonce une saisie mise en file hors connexion', async () => {
    recordReturn.mockReturnValue(of({ queued: true }));
    await open();
    const show = vi.spyOn(toast, 'show');
    await fillLine('soap', '3');

    await submit();

    expect(show).toHaveBeenCalledWith('common.queuedSaved', {}, 'info');
  });

  it('refuse une origine identique à la destination sans rien envoyer', async () => {
    await open();
    const to = select('#toLocationId');
    to.value = 'store-id';
    to.dispatchEvent(new Event('change'));
    await fillLine('soap', '1');

    await submit();

    expect(recordReturn).not.toHaveBeenCalled();
    expect(element.querySelector('#route-error')?.textContent).toContain(
      'doivent être différentes',
    );
  });

  it('refuse une quantité nulle ou à 4 décimales sans rien envoyer', async () => {
    await open();
    await fillLine('soap', '0');
    await submit();
    await fillLine('soap', '1.0005');
    await submit();

    expect(recordReturn).not.toHaveBeenCalled();
    expect(element.querySelector('#quantity-error-0')?.textContent).toContain(
      'Quantité positive, 3 décimales au maximum.',
    );
  });

  it('affiche le refus du serveur (stock insuffisant) et reste sur la page', async () => {
    const refusal = 'Stock insuffisant à cet emplacement : 12 disponible.';
    recordReturn.mockReturnValue(throwError(() => new ApiError(409, 'CONFLICT', refusal)));
    await open();
    await fillLine('soap', '20');

    await submit();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(refusal);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('ajoute et retire des lignes, sans descendre sous une ligne', async () => {
    await open();
    const add = Array.from(element.querySelectorAll<HTMLButtonElement>('button')).find((button) =>
      button.textContent?.includes('Ajouter un article'),
    ) as HTMLButtonElement;

    add.click();
    fixture.detectChanges();
    expect(element.querySelectorAll('.line')).toHaveLength(2);

    element.querySelector<HTMLButtonElement>('.remove')?.click();
    fixture.detectChanges();
    expect(element.querySelectorAll('.line')).toHaveLength(1);
    expect(element.querySelector<HTMLButtonElement>('.remove')?.disabled).toBe(true);
  });
});
