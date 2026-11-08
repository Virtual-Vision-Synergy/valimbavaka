import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import axe from 'axe-core';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { PageResponse } from '../../../../shared/models/page-response';
import { TransferDetailRDto, TransferSummaryRDto } from '../../data-access/transfer-models';
import { TRANSFER_PAGE_SIZE, TransferListPage } from './transfer-list-page';

const TRANSFERS_URL = '/api/v1/transfers';

const sellerReturn: TransferSummaryRDto = {
  id: '0195f0a0-0000-7000-8000-0000000000c1',
  number: 'RET-0015',
  origin: 'RETURN',
  status: 'RETURN',
  fromLocationId: '0195f0a0-0000-7000-8000-0000000000d1',
  fromLocationLabel: 'Boutique',
  toLocationId: '0195f0a0-0000-7000-8000-0000000000d2',
  toLocationLabel: 'Entrepôt',
  lineCount: 2,
  quantity: 5.5,
  createdAt: '2026-10-19T16:05:00Z',
  createdBy: '0195f0a0-0000-7000-8000-000000000002',
  createdByName: 'Tiana Ranaivoson',
};

const morningTransfer: TransferSummaryRDto = {
  ...sellerReturn,
  id: '0195f0a0-0000-7000-8000-0000000000c2',
  number: 'TRF-0092',
  origin: 'MORNING',
  status: 'VALIDATED',
  fromLocationId: sellerReturn.toLocationId,
  fromLocationLabel: 'Entrepôt',
  toLocationId: sellerReturn.fromLocationId,
  toLocationLabel: 'Boutique',
  createdBy: null,
  createdByName: 'Système',
};

const pageOf = (items: TransferSummaryRDto[], totalCount = items.length) =>
  ({
    items,
    page: 1,
    pageSize: TRANSFER_PAGE_SIZE,
    totalCount,
    totalPages: Math.max(1, Math.ceil(totalCount / TRANSFER_PAGE_SIZE)),
  }) satisfies PageResponse<TransferSummaryRDto>;

/** Historique des transferts (TRF-01), branché sur `GET /api/v1/transfers`. */
describe('TransferListPage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<TransferListPage>;
  let element: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransferListPage],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TransferListPage);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  const nextRequest = (): TestRequest => httpMock.expectOne((req) => req.url === TRANSFERS_URL);

  const respond = async (request: TestRequest, body: object): Promise<void> => {
    request.flush(body);
    await fixture.whenStable();
    fixture.detectChanges();
  };

  /** Applique un geste sans attendre `whenStable()` : la requête suivante reste en cours. */
  const refresh = async (): Promise<void> => {
    fixture.detectChanges();
    await Promise.resolve();
    fixture.detectChanges();
  };

  const rowOf = (number: string): string =>
    Array.from(element.querySelectorAll('tbody tr'))
      .map((row) => row.textContent ?? '')
      .find((text) => text.includes(number)) ?? '';

  it('should show the reference, date, route, status and issuer of each transfer', async () => {
    await respond(nextRequest(), pageOf([sellerReturn, morningTransfer]));

    const row = rowOf('RET-0015');
    expect(row).toContain('Boutique');
    expect(row).toContain('Entrepôt');
    expect(row).toContain('Retour');
    expect(row).toContain('Tiana Ranaivoson');
    expect(element.querySelector('tbody time')?.getAttribute('datetime')).toBe(
      '2026-10-19T16:05:00Z',
    );
    expect(element.querySelector('tbody time')?.textContent?.trim()).not.toBe('');
    expect(rowOf('TRF-0092')).toContain('Validé');
    expect(rowOf('TRF-0092')).toContain('Système');
    expect(element.querySelectorAll('thead th')).toHaveLength(7);
  });

  it('should ask the server for the first page, newest first, without origin filter', () => {
    const request = nextRequest();

    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe(String(TRANSFER_PAGE_SIZE));
    expect(request.request.params.has('origin')).toBe(false);
    expect(request.request.params.has('sortBy')).toBe(false);
    request.flush(pageOf([]));
  });

  it('should reload from the first page with the chosen origin', async () => {
    await respond(nextRequest(), pageOf([sellerReturn, morningTransfer], 20));

    Array.from(element.querySelectorAll<HTMLButtonElement>('.chips button'))
      .find((chip) => chip.textContent?.includes('Retours'))
      ?.click();
    await refresh();

    const request = nextRequest();
    expect(request.request.params.get('origin')).toBe('RETURN');
    expect(request.request.params.get('page')).toBe('1');
    request.flush(pageOf([sellerReturn]));
  });

  it('should load the next page from the server', async () => {
    await respond(nextRequest(), pageOf([sellerReturn, morningTransfer], 20));

    element.querySelector<HTMLButtonElement>('button[aria-label="Page suivante"]')?.click();
    await refresh();

    const request = nextRequest();
    expect(request.request.params.get('page')).toBe('2');
    request.flush(pageOf([]));
  });

  it('should open the full detail of a transfer when its number is clicked', async () => {
    await respond(nextRequest(), pageOf([sellerReturn, morningTransfer]));

    Array.from(element.querySelectorAll<HTMLButtonElement>('tbody .row-link'))
      .find((button) => button.textContent?.includes('RET-0015'))
      ?.click();
    await refresh();

    const detail: TransferDetailRDto = {
      ...sellerReturn,
      note: null,
      lines: [
        {
          articleId: '0195f0a0-0000-7000-8000-0000000000a1',
          articleCode: 'ART-0001',
          articleName: 'Savon au curcuma',
          unit: 'PIECE',
          quantity: 5.5,
        },
      ],
    };
    await respond(httpMock.expectOne(`${TRANSFERS_URL}/${sellerReturn.id}`), detail);

    const panel = element.querySelector('vvs-transfer-detail')?.textContent ?? '';
    expect(panel).toContain('Savon au curcuma');
    expect(panel).toContain('ART-0001');
    expect(panel).toContain('5.5 u.');
    expect(panel).toContain('Tiana Ranaivoson');
    expect(element.querySelector('tbody .row-link[aria-pressed="true"]')?.textContent?.trim()).toBe(
      'RET-0015',
    );
  });

  it('should name a location deactivated since the transfer', async () => {
    await respond(nextRequest(), pageOf([{ ...sellerReturn, toLocationLabel: null }]));

    expect(rowOf('RET-0015')).toContain('Emplacement désactivé');
  });

  it('should say so when there is no transfer yet', async () => {
    await respond(nextRequest(), pageOf([]));

    expect(element.textContent).toContain('Aucun transfert');
  });

  it('should have no detectable accessibility violation once the list is loaded', async () => {
    await respond(nextRequest(), pageOf([sellerReturn, morningTransfer]));
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
