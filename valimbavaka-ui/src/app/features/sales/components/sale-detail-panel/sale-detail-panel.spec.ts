import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { SaleDetailRDto } from '../../data-access/sales-models';
import { SaleDetailPanel } from './sale-detail-panel';

const DETAIL: SaleDetailRDto = {
  id: 'vente-42',
  number: 'VTE-0042',
  businessDate: '2026-11-11',
  createdAt: '2026-11-11T07:52:00Z',
  salesChannelId: 'canal-boutique',
  salesChannelLabel: 'Boutique',
  sellerId: 'miora',
  sellerName: 'Miora Rakoto',
  paymentMethod: 'MVOLA',
  status: 'VALIDATED',
  subtotal: 25500,
  discountAmount: 2550,
  total: 22950,
  promotion: { id: 'promo-1', name: 'Soldes de novembre', code: 'NOV10' },
  lines: [
    {
      articleId: 'savon',
      articleCode: 'ART-0001',
      articleName: 'Savon coco',
      quantity: 3,
      unitPrice: 8500,
      lineTotal: 25500,
    },
  ],
  invoice: {
    id: 'facture-1',
    number: 'FAC-2026-0042',
    issuedAt: '2026-11-11T07:52:00Z',
    status: 'VALIDATED',
  },
  commission: {
    id: 'commission-1',
    revenue: 22950,
    baseAmount: 22950,
    base: 'REVENUE',
    rate: 4,
    rateSource: 'SALES_CHANNEL',
    ruleId: 'regle-1',
    ruleName: 'Boutique 4 %',
    amount: 918,
    status: 'CALCULATED',
  },
};

/** Détail d'une vente (VTE-09) : lignes, promotion, facture et règle de commission appliquée. */
describe('SaleDetailPanel', () => {
  let fixture: ComponentFixture<SaleDetailPanel>;
  let httpMock: HttpTestingController;
  let element: HTMLElement;

  const text = (selector: string): string =>
    element
      .querySelector(selector)
      ?.textContent?.replace(/[\t\n ]+/g, ' ')
      .trim() ?? '';

  async function load(detail: SaleDetailRDto): Promise<void> {
    httpMock.expectOne('/api/v1/sales/vente-42/detail').flush(detail);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SaleDetailPanel],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTestTranslations()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(SaleDetailPanel);
    fixture.componentRef.setInput('saleId', 'vente-42');
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('affiche les lignes avec leur prix unitaire, la promotion et le numéro de facture', async () => {
    await load(DETAIL);

    expect(text('#sale-detail-title .mono')).toBe('VTE-0042');
    expect(text('tbody')).toContain('Savon coco');
    expect(text('tbody')).toContain('ART-0001');
    expect(text('.facts')).toContain('Soldes de novembre');
    expect(text('.facts')).toContain('NOV10');
    expect(text('.facts')).toContain('FAC-2026-0042');
  });

  it('affiche la règle de commission appliquée : origine du taux, nom de la règle, base, taux et montant', async () => {
    await load(DETAIL);

    const commission = text('.commission');
    expect(commission).toContain('Règle du canal');
    expect(commission).toContain('Boutique 4 %');
    expect(commission).toContain("Chiffre d'affaires");
    expect(commission).toContain('4 %');
    expect(commission).toContain('À valider');
  });

  it('sans règle de canal, indique le taux de la vendeuse sans nom de règle', async () => {
    await load({
      ...DETAIL,
      commission: {
        ...DETAIL.commission!,
        rateSource: 'SELLER',
        ruleId: null,
        ruleName: null,
      },
    });

    expect(text('.commission')).toContain('Taux de la vendeuse');
    expect(text('.commission')).not.toContain('·');
  });

  it("explique l'absence de commission, de promotion et de facture", async () => {
    await load({ ...DETAIL, commission: null, promotion: null, invoice: null, discountAmount: 0 });

    expect(text('.commission .note')).toContain('Aucune commission');
    expect(text('.facts')).toContain('Aucune promotion');
    expect(text('.facts')).toContain('Aucune facture émise');
  });

  it("propose de réessayer quand le serveur refuse la vente (404 d'une vente de collègue)", async () => {
    httpMock
      .expectOne('/api/v1/sales/vente-42/detail')
      .flush({ code: 'NOT_FOUND' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('#sale-detail-title')).toBeNull();
    expect(element.querySelector('vvs-list-state')).not.toBeNull();
  });
});
