import { HttpHeaders, HttpParams, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ToastService } from '../components/toast/toast-service';
import { ExportService } from './export-service';

const LIST_URL = '/api/v1/sales';

/** Brique d'export de SYS-01 : bonne route jumelle, mêmes critères, fichier téléchargé et confirmé. */
describe('ExportService', () => {
  let sut: ExportService;
  let httpMock: HttpTestingController;
  let toast: ToastService;
  let anchorClick: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    sut = TestBed.inject(ExportService);
    httpMock = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService);
    URL.createObjectURL = vi.fn(() => 'blob:export');
    URL.revokeObjectURL = vi.fn();
    anchorClick = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);
  });

  afterEach(() => {
    httpMock.verify();
    anchorClick.mockRestore();
  });

  it('télécharge le xlsx de la route /export et confirme avec le nom annoncé par le serveur', () => {
    let filename: string | undefined;

    sut.download(LIST_URL, 'xlsx').subscribe((name) => (filename = name));
    httpMock.expectOne(`${LIST_URL}/export`).flush(new Blob(['xlsx']), {
      headers: new HttpHeaders({ 'Content-Disposition': 'attachment; filename="ventes.xlsx"' }),
    });

    expect(filename).toBe('ventes.xlsx');
    expect(anchorClick).toHaveBeenCalledTimes(1);
    expect(toast.toasts()).toEqual([
      expect.objectContaining({
        messageKey: 'export.done',
        params: { filename: 'ventes.xlsx' },
        tone: 'success',
      }),
    ]);
  });

  it('appelle /export/pdf avec les critères de la liste affichée', () => {
    const params = new HttpParams().set('from', '2026-11-01').set('to', '2026-11-30');

    sut.download(`${LIST_URL}/`, 'pdf', params).subscribe();
    const request = httpMock.expectOne((req) => req.url === `${LIST_URL}/export/pdf`);
    request.flush(new Blob(['%PDF']));

    expect(request.request.params.get('from')).toBe('2026-11-01');
    expect(request.request.params.get('to')).toBe('2026-11-30');
    expect(request.request.responseType).toBe('blob');
    expect(toast.toasts()[0]).toEqual(
      expect.objectContaining({ messageKey: 'export.done', params: { filename: 'export.pdf' } }),
    );
  });

  it('signale un export trop volumineux (422) sans rien télécharger', () => {
    let failed = false;

    sut.download(LIST_URL, 'xlsx').subscribe({ error: () => (failed = true) });
    httpMock
      .expectOne(`${LIST_URL}/export`)
      .flush(new Blob(['{}']), { status: 422, statusText: 'Unprocessable Content' });

    expect(failed).toBe(true);
    expect(anchorClick).not.toHaveBeenCalled();
    expect(toast.toasts()[0]).toEqual(
      expect.objectContaining({ messageKey: 'export.tooLarge', tone: 'danger' }),
    );
  });

  it('signale un refus du serveur (403)', () => {
    sut.download(LIST_URL, 'pdf').subscribe({ error: () => undefined });
    httpMock
      .expectOne(`${LIST_URL}/export/pdf`)
      .flush(new Blob(['{}']), { status: 403, statusText: 'Forbidden' });

    expect(toast.toasts()[0]).toEqual(
      expect.objectContaining({ messageKey: 'export.forbidden', tone: 'danger' }),
    );
  });

  it('signale toute autre erreur par un message générique', () => {
    sut.download(LIST_URL, 'xlsx').subscribe({ error: () => undefined });
    httpMock.expectOne(`${LIST_URL}/export`).error(new ProgressEvent('error'));

    expect(toast.toasts()[0]).toEqual(
      expect.objectContaining({ messageKey: 'export.failed', tone: 'danger' }),
    );
  });
});
