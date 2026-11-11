import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { SalesBookExport } from './sales-book-export';

const BOOK_URL = '/api/v1/invoices/sales-book';

/** Export du livre des ventes (FAC-06) : période bornée, bonne route, confirmation, échec sans blocage. */
describe('SalesBookExport', () => {
  let fixture: ComponentFixture<SalesBookExport>;
  let httpMock: HttpTestingController;
  let toast: ToastService;
  let element: HTMLElement;
  let anchorClick: ReturnType<typeof vi.spyOn>;

  const dateInputs = (): HTMLInputElement[] =>
    Array.from(element.querySelectorAll<HTMLInputElement>('input[type="date"]'));
  const buttons = (): HTMLButtonElement[] =>
    Array.from(element.querySelectorAll<HTMLButtonElement>('button'));
  const setPeriod = (from: string, to: string): void => {
    const [fromInput, toInput] = dateInputs();
    fromInput.value = from;
    fromInput.dispatchEvent(new Event('change'));
    toInput.value = to;
    toInput.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };

  beforeEach(async () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 10, 18, 10, 0, 0));
    URL.createObjectURL = vi.fn(() => 'blob:book');
    URL.revokeObjectURL = vi.fn();
    anchorClick = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);
    await TestBed.configureTestingModule({
      imports: [SalesBookExport],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTestTranslations()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService);
    fixture = TestBed.createComponent(SalesBookExport);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    anchorClick.mockRestore();
    vi.useRealTimers();
  });

  it('propose le mois en cours par défaut', () => {
    const [from, to] = dateInputs();

    expect(from.value).toBe('2026-11-01');
    expect(to.value).toBe('2026-11-18');
  });

  it('exporte le xlsx de la période choisie et confirme la génération du fichier', () => {
    setPeriod('2026-10-01', '2026-10-31');

    buttons()[0].click();
    const request = httpMock.expectOne((req) => req.url === `${BOOK_URL}/export`);
    request.flush(new Blob(['xlsx']));

    expect(request.request.params.get('from')).toBe('2026-10-01');
    expect(request.request.params.get('to')).toBe('2026-10-31');
    expect(anchorClick).toHaveBeenCalledTimes(1);
    expect(toast.toasts()).toEqual([
      expect.objectContaining({ messageKey: 'export.done', tone: 'success' }),
    ]);
  });

  it('exporte le PDF par la route jumelle /export/pdf', () => {
    buttons()[1].click();

    httpMock.expectOne((req) => req.url === `${BOOK_URL}/export/pdf`).flush(new Blob(['%PDF']));

    expect(anchorClick).toHaveBeenCalledTimes(1);
  });

  it("n'envoie rien pour une période à l'envers et prévient", () => {
    setPeriod('2026-11-30', '2026-11-01');

    expect(buttons().every((button) => button.disabled)).toBe(true);
    expect(element.querySelector('[role="alert"]')).not.toBeNull();
    buttons()[0].click();
    httpMock.expectNone(`${BOOK_URL}/export`);
  });

  it('accepte 366 jours mais refuse 367 jours', () => {
    setPeriod('2025-01-01', '2026-01-01');
    expect(buttons().some((button) => button.disabled)).toBe(false);

    setPeriod('2025-01-01', '2026-01-02');
    expect(buttons().every((button) => button.disabled)).toBe(true);
  });

  it('refuse une date vidée', () => {
    setPeriod('', '2026-11-18');

    expect(buttons().every((button) => button.disabled)).toBe(true);
  });

  it("signale l'échec et rend la main pour un nouvel essai", () => {
    buttons()[0].click();
    httpMock
      .expectOne((req) => req.url === `${BOOK_URL}/export`)
      .flush(new Blob(['{}']), { status: 403, statusText: 'Forbidden' });
    fixture.detectChanges();

    expect(toast.toasts()).toEqual([
      expect.objectContaining({ messageKey: 'export.forbidden', tone: 'danger' }),
    ]);
    expect(buttons()[0].disabled).toBe(false);
  });
});
