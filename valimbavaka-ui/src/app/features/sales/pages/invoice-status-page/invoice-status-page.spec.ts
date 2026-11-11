import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { InvoiceStatusTotalsRDto } from '../../data-access/invoice-status-models';
import { InvoiceStatusPage } from './invoice-status-page';

const URL = '/api/v1/invoices/status-totals';
const TOTALS: InvoiceStatusTotalsRDto = {
  from: '2026-11-01',
  to: '2026-11-30',
  invoiceCount: 4,
  amount: 8000,
  statuses: [
    { status: 'VALIDATED', invoiceCount: 2, amount: 3500 },
    { status: 'CANCELLED', invoiceCount: 1, amount: 500 },
    { status: 'REVERSED', invoiceCount: 1, amount: 4000 },
  ],
};

/** Suivi des statuts de facture (FAC-05) : nombre et montant par statut, et total consolidé. */
describe('InvoiceStatusPage', () => {
  let httpMock: HttpTestingController;

  const open = async (): Promise<HTMLElement> => {
    await TestBed.configureTestingModule({
      imports: [InvoiceStatusPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTestTranslations(),
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
    const fixture = TestBed.createComponent(InvoiceStatusPage);
    fixture.detectChanges();
    httpMock.expectOne((req) => req.url === URL).flush(TOTALS);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  };

  const text = (node: Element): string => (node.textContent ?? '').replace(/\s+/g, ' ').trim();

  afterEach(() => httpMock.verify());

  it('should show the number of invoices of each status', async () => {
    const element = await open();

    const rows = Array.from(element.querySelectorAll('tbody tr')).map(text);
    expect(rows).toHaveLength(3);
    expect(rows[0]).toContain('2');
    expect(rows[1]).toContain('1');
    expect(rows[2]).toContain('1');
  });

  it('should show the consolidated total of the period', async () => {
    const element = await open();

    expect(text(element.querySelector('.totals') as Element)).toContain('4');
    expect(text(element.querySelector('tfoot tr') as Element)).toContain('4');
  });
});
