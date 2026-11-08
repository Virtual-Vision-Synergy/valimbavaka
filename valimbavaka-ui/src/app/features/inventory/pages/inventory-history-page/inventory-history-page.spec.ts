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
import { InventorySessionRDto } from '../../data-access/inventory-models';
import { InventoryHistoryPage } from './inventory-history-page';

const INVENTORIES_URL = '/api/v1/inventories';
const CLOSED: InventorySessionRDto = {
  id: '0195f0a0-0000-7000-8000-0000000000c1',
  number: 'INV-0012',
  locationId: 'loc-1',
  locationCode: 'STORE',
  locationLabel: 'Boutique',
  scope: 'ALL',
  rangeId: null,
  responsibleId: 'user-1',
  responsibleName: 'Hanta Rakoto',
  countDate: '2026-11-08',
  status: 'CLOSED',
  openedAt: '2026-11-08T06:30:00Z',
  openedBy: 'user-1',
  closedAt: '2026-11-08T11:30:00Z',
  lineCount: 40,
  gapCount: 3,
  version: 2,
};
const IN_PROGRESS: InventorySessionRDto = {
  ...CLOSED,
  id: '0195f0a0-0000-7000-8000-0000000000c2',
  number: 'INV-0013',
  locationLabel: 'Entrepôt',
  status: 'IN_PROGRESS',
  closedAt: null,
  lineCount: 12,
  gapCount: 0,
};
const pageOf = (items: InventorySessionRDto[], totalCount = items.length) => ({
  items,
  page: 1,
  pageSize: 20,
  totalCount,
  totalPages: Math.max(1, Math.ceil(totalCount / 20)),
});

/** INV-05 : chaque session est consultable avec son statut et son nombre d'écarts. */
describe('InventoryHistoryPage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<InventoryHistoryPage>;
  let element: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InventoryHistoryPage],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(InventoryHistoryPage);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  const nextRequest = (): TestRequest => httpMock.expectOne((req) => req.url === INVENTORIES_URL);

  const respond = async (request: TestRequest, body: object): Promise<void> => {
    request.flush(body);
    await fixture.whenStable();
    fixture.detectChanges();
  };

  const chooseStatus = async (label: string): Promise<void> => {
    Array.from(element.querySelectorAll<HTMLButtonElement>('.chips button'))
      .find((chip) => chip.textContent?.includes(label))
      ?.click();
    fixture.detectChanges();
    await Promise.resolve();
    fixture.detectChanges();
  };

  const rows = (): string[][] =>
    Array.from(element.querySelectorAll('tbody tr')).map((row) =>
      Array.from(row.querySelectorAll('th, td')).map((cell) => cell.textContent?.trim() ?? ''),
    );

  it('charge toutes les sessions, les plus récentes d’abord, sans filtre de statut', async () => {
    const request = nextRequest();

    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('20');
    expect(request.request.params.get('sortBy')).toBe('createdAt');
    expect(request.request.params.get('sortDirection')).toBe('DESC');
    expect(request.request.params.has('status')).toBe(false);
    await respond(request, pageOf([IN_PROGRESS, CLOSED]));
    const titles = Array.from(element.querySelectorAll('tbody .cell-title'));
    expect(titles.map((cell) => cell.textContent?.trim())).toEqual(['Entrepôt', 'Boutique']);
    expect(element.querySelector('tbody .cell-sub')?.textContent).toContain('INV-0013');
  });

  it('affiche le statut, le nombre d’articles et le nombre d’écarts de chaque session', async () => {
    await respond(nextRequest(), pageOf([IN_PROGRESS, CLOSED]));

    const [inProgress, closed] = rows();
    expect(inProgress[2]).toBe('—');
    expect(inProgress[3]).toBe('En cours');
    expect(inProgress.slice(4)).toEqual(['12', '0']);
    expect(closed[3]).toBe('Clôturée');
    expect(closed.slice(4)).toEqual(['40', '3']);
    expect(element.querySelector('.gaps.has-gaps')?.textContent?.trim()).toBe('3');
  });

  it('recharge avec le statut choisi', async () => {
    await respond(nextRequest(), pageOf([IN_PROGRESS, CLOSED]));

    await chooseStatus('Clôturée');

    const request = nextRequest();
    expect(request.request.params.get('status')).toBe('CLOSED');
    expect(request.request.params.get('page')).toBe('1');
    await respond(request, pageOf([CLOSED]));
    expect(rows()).toHaveLength(1);
  });

  it('indique qu’aucune session ne correspond au filtre', async () => {
    await respond(nextRequest(), pageOf([]));

    expect(element.textContent).toContain("Aucune session d'inventaire pour ce filtre.");
  });

  it('ne présente aucune violation d’accessibilité détectable', async () => {
    await respond(nextRequest(), pageOf([IN_PROGRESS, CLOSED]));
    document.body.appendChild(element);

    const results = await axe.run(element, {
      runOnly: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'],
      rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
    });

    expect(results.violations.map((violation) => violation.id)).toEqual([]);
    element.remove();
  });
});
