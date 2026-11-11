import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { InvoiceDelivery } from '../../data-access/invoice-delivery';
import { InvoiceRDto, SaleSummaryRDto } from '../../data-access/sales-models';
import { InvoicePanel } from './invoice-panel';

const SALE: SaleSummaryRDto = {
  id: 'vente-42',
  number: 'VTE-0042',
  businessDate: '2026-11-08',
  createdAt: '2026-11-08T07:52:00Z',
  salesChannelId: 'canal-boutique',
  salesChannelLabel: 'Boutique',
  sellerId: 'miora',
  sellerName: 'Miora Rakoto',
  paymentMethod: 'MVOLA',
  status: 'VALIDATED',
  subtotal: 17000,
  discountAmount: 0,
  total: 17000,
  lines: [{ articleId: 'savon', articleName: 'Savon coco', quantity: 2, lineTotal: 17000 }],
};

const INVOICE: InvoiceRDto = {
  id: 'facture-1',
  number: 'FAC-2026-0042',
  saleId: 'vente-42',
  saleNumber: 'VTE-0042',
  issuedAt: '2026-11-08T07:52:00Z',
  businessDate: '2026-11-08',
  status: 'VALIDATED',
  salesChannelLabel: 'Boutique',
  sellerId: 'miora',
  sellerName: 'Miora Rakoto',
  paymentMethod: 'MVOLA',
  issuerName: 'By Valimbavaka Cosmetics',
  issuerNif: null,
  subtotal: 17000,
  discountAmount: 0,
  total: 17000,
  lines: [
    { articleId: 'savon', label: 'Savon coco', quantity: 2, unitPrice: 8500, lineTotal: 17000 },
  ],
};

/** Panneau facture (FAC-01) : la facture émise par le serveur, ou l'aperçu d'une vente qui n'en a pas. */
describe('InvoicePanel', () => {
  let fixture: ComponentFixture<InvoicePanel>;
  let httpMock: HttpTestingController;
  let element: HTMLElement;
  const delivery = {
    save: vi.fn(),
    canShare: vi.fn(() => false),
    share: vi.fn(() => Promise.resolve()),
    openMail: vi.fn(),
  };

  const text = (selector: string): string =>
    element
      .querySelector(selector)
      ?.textContent?.replace(/[\t\n ]+/g, ' ')
      .trim() ?? '';

  beforeEach(async () => {
    vi.restoreAllMocks();
    delivery.save.mockReset();
    delivery.canShare.mockReset().mockReturnValue(false);
    delivery.share.mockReset().mockResolvedValue(undefined);
    delivery.openMail.mockReset();
    vi.spyOn(window, 'print').mockImplementation(() => undefined);
    await TestBed.configureTestingModule({
      imports: [InvoicePanel],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTestTranslations(),
        { provide: InvoiceDelivery, useValue: delivery },
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(InvoicePanel);
    fixture.componentRef.setInput('sale', SALE);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it("affiche le numéro de la facture émise et les lignes figées à l'émission", async () => {
    httpMock.expectOne('/api/v1/invoices/sales/vente-42').flush(INVOICE);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(text('#invoice-title .mono')).toBe('FAC-2026-0042');
    expect(text('.paper .ident .mono')).toBe('FAC-2026-0042');
    expect(text('.paper .lines tbody')).toContain('Savon coco');
    expect(element.querySelector('.note')?.textContent).not.toContain('Aucune facture');
  });

  it("montre l'aperçu « À émettre » et son explication quand la vente n'a pas de facture", async () => {
    httpMock
      .expectOne('/api/v1/invoices/sales/vente-42')
      .flush({ code: 'NOT_FOUND' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(text('#invoice-title .mono')).toBe('VTE-0042');
    expect(text('.paper .ident .mono')).toBe('À émettre');
    expect(text('.note')).toContain("Aucune facture n'a été émise");
  });

  const regenerateButton = (): HTMLButtonElement =>
    Array.from(element.querySelectorAll<HTMLButtonElement>('button')).find(
      (button) => button.textContent?.trim() === 'Régénérer',
    ) as HTMLButtonElement;

  it('régénère la facture sans changer son numéro et prévient la vendeuse', async () => {
    httpMock.expectOne('/api/v1/invoices/sales/vente-42').flush(INVOICE);
    await fixture.whenStable();
    fixture.detectChanges();

    regenerateButton().click();
    httpMock.expectOne('/api/v1/invoices/sales/vente-42/regenerate').flush(INVOICE);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(text('#invoice-title .mono')).toBe('FAC-2026-0042');
    expect(TestBed.inject(ToastService).toasts()[0].messageKey).toBe('sales.invoice.regenerated');
    expect(regenerateButton().disabled).toBe(false);
  });

  it("émet la facture d'une vente qui n'en avait pas et remplace l'aperçu", async () => {
    httpMock
      .expectOne('/api/v1/invoices/sales/vente-42')
      .flush({ code: 'NOT_FOUND' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(text('.paper .ident .mono')).toBe('À émettre');

    regenerateButton().click();
    httpMock.expectOne('/api/v1/invoices/sales/vente-42/regenerate').flush(INVOICE);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(text('.paper .ident .mono')).toBe('FAC-2026-0042');
    expect(element.querySelector('.note')?.textContent).not.toContain('Aucune facture');
  });

  it("désactive le bouton pendant la régénération et signale l'échec", async () => {
    httpMock.expectOne('/api/v1/invoices/sales/vente-42').flush(INVOICE);
    await fixture.whenStable();
    fixture.detectChanges();

    regenerateButton().click();
    fixture.detectChanges();
    expect(regenerateButton().disabled).toBe(true);
    httpMock
      .expectOne('/api/v1/invoices/sales/vente-42/regenerate')
      .flush({ code: 'NOT_FOUND' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(TestBed.inject(ToastService).toasts()[0].messageKey).toBe(
      'sales.invoice.regenerateFailed',
    );
    expect(regenerateButton().disabled).toBe(false);
  });

  const button = (label: string): HTMLButtonElement =>
    Array.from(element.querySelectorAll<HTMLButtonElement>('button')).find(
      (candidate) => candidate.textContent?.trim() === label,
    ) as HTMLButtonElement;

  const loadInvoice = async (): Promise<void> => {
    httpMock.expectOne('/api/v1/invoices/sales/vente-42').flush(INVOICE);
    await fixture.whenStable();
    fixture.detectChanges();
  };

  const flushPdf = async (): Promise<void> => {
    const request = httpMock.expectOne('/api/v1/invoices/facture-1/pdf');
    expect(request.request.method).toBe('GET');
    expect(request.request.responseType).toBe('blob');
    request.flush(new Blob(['%PDF'], { type: 'application/pdf' }));
    await fixture.whenStable();
    fixture.detectChanges();
  };

  const lastToast = (): string => TestBed.inject(ToastService).toasts().at(-1)?.messageKey ?? '';

  it("imprime la facture et confirme l'impression", async () => {
    await loadInvoice();

    button('Imprimer').click();

    expect(window.print).toHaveBeenCalledOnce();
    expect(lastToast()).toBe('sales.invoice.printed');
  });

  it('enregistre le PDF de la facture émise sous le nom de son numéro et le confirme', async () => {
    await loadInvoice();

    button('PDF').click();
    await flushPdf();

    expect(delivery.save).toHaveBeenCalledOnce();
    const [file, filename] = delivery.save.mock.calls[0] as unknown as [Blob, string];
    expect(file.type).toBe('application/pdf');
    expect(filename).toBe('FAC-2026-0042.pdf');
    expect(lastToast()).toBe('sales.invoice.pdfSaved');
    expect(button('PDF').disabled).toBe(false);
  });

  it("signale l'échec du PDF sans rien enregistrer", async () => {
    await loadInvoice();

    button('PDF').click();
    httpMock
      .expectOne('/api/v1/invoices/facture-1/pdf')
      .flush(new Blob(['{}']), { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(delivery.save).not.toHaveBeenCalled();
    expect(lastToast()).toBe('sales.invoice.pdfFailed');
    expect(button('PDF').disabled).toBe(false);
  });

  it('partage le PDF par le système quand le navigateur sait le faire', async () => {
    delivery.canShare.mockReturnValue(true);
    await loadInvoice();

    button('Envoyer').click();
    await flushPdf();

    expect(delivery.share).toHaveBeenCalledOnce();
    expect(delivery.openMail).not.toHaveBeenCalled();
    expect(lastToast()).toBe('sales.invoice.shared');
  });

  it('ne confirme rien quand la vendeuse abandonne le menu de partage', async () => {
    delivery.canShare.mockReturnValue(true);
    delivery.share.mockRejectedValue(new DOMException('annulé', 'AbortError'));
    await loadInvoice();

    button('Envoyer').click();
    await flushPdf();

    expect(delivery.share).toHaveBeenCalledOnce();
    expect(TestBed.inject(ToastService).toasts()).toHaveLength(0);
  });

  it("ouvre un message prérempli et enregistre le PDF à joindre quand le partage n'existe pas", async () => {
    await loadInvoice();

    button('Envoyer').click();
    await flushPdf();

    expect(delivery.save).toHaveBeenCalledOnce();
    const [subject, body] = delivery.openMail.mock.calls[0] as unknown as [string, string];
    expect(subject).toBe('Facture FAC-2026-0042 · By Valimbavaka Cosmetics');
    expect(body).toContain('FAC-2026-0042');
    expect(body).toContain('08/11/2026');
    expect(lastToast()).toBe('sales.invoice.mailOpened');
  });

  it("désactive PDF et Envoyer tant que la vente n'a pas de facture émise", async () => {
    httpMock
      .expectOne('/api/v1/invoices/sales/vente-42')
      .flush({ code: 'NOT_FOUND' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(button('PDF').disabled).toBe(true);
    expect(button('Envoyer').disabled).toBe(true);
    expect(button('Imprimer').disabled).toBe(false);
    expect(element.textContent).toContain('Régénérer');
  });
});
