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
import { SupplierRDto } from '../../../supplier/data-access/supplier-models';
import { SupplierService } from '../../../supplier/data-access/supplier-service';
import { RestockOrderRDto, StockRDto, SupplierReceiptRDto } from '../../data-access/stock-models';
import { StockService } from '../../data-access/stock-service';
import { SupplierReceiptPage } from './supplier-receipt-page';

const LOCATIONS = [
  { id: 'store-id', code: 'STORE', label: 'Boutique' },
  { id: 'warehouse-id', code: 'WAREHOUSE', label: 'Entrepôt' },
];

function supplier(id: string, name: string, active: boolean): SupplierRDto {
  return { id, name, active } as SupplierRDto;
}

function stock(articleId: string, name: string, cost: number | null): StockRDto {
  return {
    articleId,
    code: `ART-${articleId}`,
    name,
    rangeId: null,
    unit: 'PIECE',
    threshold: 40,
    cost,
    total: 0,
    locations: [],
  };
}

const ORDER = {
  id: 'order-id',
  reference: 'BR-000007',
  status: 'PENDING',
  createdAt: '2026-11-08T08:00:00Z',
  lines: [
    { lineNumber: 1, articleId: 'soap', quantity: 40, supplierId: 'ravo' },
    { lineNumber: 2, articleId: 'oil', quantity: 20, supplierId: 'ravo' },
  ],
} as unknown as RestockOrderRDto;

const RECORDED = { id: 'receipt-id', number: 'REC-000012' } as SupplierReceiptRDto;

describe('SupplierReceiptPage', () => {
  const recordReceipt = vi.fn();
  let fixture: ComponentFixture<SupplierReceiptPage>;
  let element: HTMLElement;
  let navigate: MockInstance<Router['navigate']>;
  let toast: ToastService;

  const open = async (): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [SupplierReceiptPage],
      providers: [
        provideRouter([]),
        provideTestTranslations(),
        {
          provide: SupplierService,
          useValue: {
            list: () =>
              of({
                items: [supplier('ravo', 'Huiles Ravo', true), supplier('old', 'Ancien', false)],
              }),
          },
        },
        { provide: ReferentialService, useValue: { listStockLocations: () => of(LOCATIONS) } },
        {
          provide: StockService,
          useValue: {
            recordReceipt,
            pendingRestockOrders: () => of([ORDER]),
            quantities: () =>
              of([stock('soap', 'Savon noir', 3120.5), stock('oil', 'Huile de coco', null)]),
          },
        },
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    toast = TestBed.inject(ToastService);
    fixture = TestBed.createComponent(SupplierReceiptPage);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const select = (selector: string): HTMLSelectElement =>
    element.querySelector<HTMLSelectElement>(selector) as HTMLSelectElement;

  const input = (selector: string): HTMLInputElement =>
    element.querySelector<HTMLInputElement>(selector) as HTMLInputElement;

  const choose = async (selector: string, value: string): Promise<void> => {
    const field = select(selector);
    field.value = value;
    field.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const type = async (selector: string, value: string): Promise<void> => {
    const field = input(selector);
    field.value = value;
    field.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const submit = async (): Promise<void> => {
    element.querySelector<HTMLFormElement>('form')?.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const fillFreeEntry = async (quantity: string, unitCost: string): Promise<void> => {
    await choose('#supplierId', 'ravo');
    await choose('#article-0', 'soap');
    await type('#quantity-0', quantity);
    await type('#cost-0', unitCost);
  };

  beforeEach(() => recordReceipt.mockReset());

  it('reçoit par défaut à l’entrepôt et ne propose que les fournisseurs actifs', async () => {
    await open();

    expect(select('#locationId').value).toBe('warehouse-id');
    const suppliers = Array.from(select('#supplierId').options).map((option) => option.text.trim());
    expect(suppliers).toContain('Huiles Ravo');
    expect(suppliers).not.toContain('Ancien');
  });

  it('préremplit les lignes et le fournisseur en reprenant un bon de réapprovisionnement', async () => {
    await open();

    await choose('#restockOrderId', 'order-id');

    expect(select('#supplierId').value).toBe('ravo');
    expect(select('#article-1').value).toBe('oil');
    expect(input('#quantity-0').value).toBe('40');
    expect(input('#cost-0').value).toBe('3120.5');
    expect(input('#cost-1').value).toBe('');
  });

  it('envoie la réception, annonce son numéro et revient aux stocks', async () => {
    recordReceipt.mockReturnValue(of({ queued: false, body: RECORDED }));
    await open();
    const show = vi.spyOn(toast, 'show');
    await fillFreeEntry('12,5', '3000');

    await submit();

    expect(recordReceipt).toHaveBeenCalledWith({
      supplierId: 'ravo',
      locationId: 'warehouse-id',
      restockOrderId: null,
      lines: [{ articleId: 'soap', quantity: 12.5, unitCost: 3000 }],
      note: null,
    });
    expect(show).toHaveBeenCalledWith('stocks.receipt.saved', { number: 'REC-000012' });
    expect(navigate).toHaveBeenCalledWith(['/stocks']);
  });

  it('refuse une quantité nulle ou un coût négatif sans rien envoyer', async () => {
    await open();
    await fillFreeEntry('0', '-1');

    await submit();

    expect(recordReceipt).not.toHaveBeenCalled();
    expect(element.querySelector('#quantity-error-0')?.textContent).toContain(
      'Quantité supérieure à zéro',
    );
    expect(element.querySelector('#cost-error-0')?.textContent).toContain('Coût positif ou nul');
  });

  it('affiche le refus du serveur (bon déjà reçu) et reste sur la page', async () => {
    const refusal = 'Le bon de réapprovisionnement BR-000007 est déjà réceptionné.';
    recordReceipt.mockReturnValue(throwError(() => new ApiError(409, 'CONFLICT', refusal)));
    await open();
    await fillFreeEntry('1', '0');

    await submit();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(refusal);
    expect(navigate).not.toHaveBeenCalled();
  });
});
