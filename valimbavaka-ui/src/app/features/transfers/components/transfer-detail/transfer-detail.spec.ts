import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import axe from 'axe-core';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { TransferDetailRDto } from '../../data-access/transfer-models';
import { TransferDetail } from './transfer-detail';

const TRANSFER_ID = '0195f0a0-0000-7000-8000-0000000000c1';
const DETAIL_URL = `/api/v1/transfers/${TRANSFER_ID}`;

const sellerReturn: TransferDetailRDto = {
  id: TRANSFER_ID,
  number: 'RET-0015',
  origin: 'RETURN',
  status: 'RETURN',
  fromLocationId: '0195f0a0-0000-7000-8000-0000000000d1',
  fromLocationLabel: 'Boutique',
  toLocationId: '0195f0a0-0000-7000-8000-0000000000d2',
  toLocationLabel: 'Entrepôt',
  note: 'Invendus du samedi',
  lines: [
    {
      articleId: '0195f0a0-0000-7000-8000-0000000000a1',
      articleCode: 'ART-0002',
      articleName: 'Huile de coco',
      unit: 'LITER',
      quantity: 2.5,
    },
    {
      articleId: '0195f0a0-0000-7000-8000-0000000000a2',
      articleCode: 'ART-0001',
      articleName: 'Savon au curcuma',
      unit: 'PIECE',
      quantity: 3,
    },
  ],
  quantity: 5.5,
  createdAt: '2026-10-19T16:05:00Z',
  createdBy: '0195f0a0-0000-7000-8000-000000000002',
  createdByName: 'Tiana Ranaivoson',
};

/** Détail d'un bon de transfert (TRF-04), branché sur `GET /api/v1/transfers/{id}`. */
describe('TransferDetail', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<TransferDetail>;
  let element: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransferDetail],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideTestTranslations(),
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TransferDetail);
    fixture.componentRef.setInput('transferId', TRANSFER_ID);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  const respond = async (body: TransferDetailRDto): Promise<void> => {
    httpMock.expectOne(DETAIL_URL).flush(body);
    await fixture.whenStable();
    fixture.detectChanges();
  };

  /** Applique un geste sans attendre `whenStable()` : la requête suivante reste en cours. */
  const refresh = async (): Promise<void> => {
    fixture.detectChanges();
    await Promise.resolve();
    fixture.detectChanges();
  };

  const lineTexts = (): string[] =>
    Array.from(element.querySelectorAll('.lines li')).map((line) =>
      (line.textContent ?? '').replace(/\s+/g, ' ').trim(),
    );

  it('should show every article with its code, quantity and unit, in the entered order', async () => {
    await respond(sellerReturn);

    expect(lineTexts()).toEqual(['Huile de coco ART-0002 2.5 L', 'Savon au curcuma ART-0001 3 u.']);
  });

  it('should show the reference, status, route, date, issuer and note of the transfer', async () => {
    await respond(sellerReturn);

    const head = element.querySelector('.head')?.textContent ?? '';
    expect(element.querySelector('h2')?.textContent).toContain('RET-0015');
    expect(head).toContain('Retour');
    expect(head).toContain('Boutique');
    expect(head).toContain('Entrepôt');
    expect(head).toContain('−5.5 u.');
    expect(head).toContain('Tiana Ranaivoson');
    expect(head).toContain('Invendus du samedi');
    expect(element.querySelector('.head time')?.getAttribute('datetime')).toBe(
      '2026-10-19T16:05:00Z',
    );
  });

  it('should name a deactivated location and leave out an empty note', async () => {
    await respond({ ...sellerReturn, fromLocationLabel: null, note: null });

    expect(element.querySelector('.head')?.textContent).toContain('Emplacement désactivé');
    expect(element.querySelector('.note')).toBeNull();
  });

  it('should load the transfer again when another one is selected', async () => {
    await respond(sellerReturn);

    fixture.componentRef.setInput('transferId', 'other-id');
    await refresh();

    httpMock.expectOne('/api/v1/transfers/other-id').flush({ ...sellerReturn, number: 'TRF-0092' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(element.querySelector('h2')?.textContent).toContain('TRF-0092');
  });

  it('should offer to retry when the transfer cannot be loaded', async () => {
    httpMock
      .expectOne(DETAIL_URL)
      .flush(
        { status: 404, code: 'NOT_FOUND', detail: 'Transfert introuvable' },
        { status: 404, statusText: 'Not Found' },
      );
    await fixture.whenStable();
    fixture.detectChanges();

    const retry = Array.from(element.querySelectorAll('button')).find((button) =>
      button.textContent?.includes('Réessayer'),
    );
    expect(retry).toBeDefined();
    retry?.click();
    await refresh();
    await respond(sellerReturn);
    expect(element.querySelector('h2')?.textContent).toContain('RET-0015');
  });

  it('should have no detectable accessibility violation once loaded', async () => {
    await respond(sellerReturn);
    document.body.appendChild(element);

    const results = await axe.run(element, {
      runOnly: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'],
      rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
    });

    expect(
      results.violations.map((violation) => `${violation.id}: ${violation.nodes[0]?.html}`),
    ).toEqual([]);
    element.remove();
  });
});
