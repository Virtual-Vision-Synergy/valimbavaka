import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import axe from 'axe-core';
import { vi } from 'vitest';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ConnectivityService } from '../../../../core/offline/connectivity-service';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { InventoryLineRDto, InventorySessionRDto } from '../../data-access/inventory-models';
import { InventoryPage } from './inventory-page';

const INVENTORIES_URL = '/api/v1/inventories';
const SESSION_ID = '0195f0a0-0000-7000-8000-0000000000c1';
const LINE_ID = '0195f0a0-0000-7000-8000-0000000000d1';
const COUNT_URL = `${INVENTORIES_URL}/${SESSION_ID}/lines/${LINE_ID}/count`;
const page = <T>(items: T[]) => ({
  items,
  page: 1,
  pageSize: 100,
  totalCount: items.length,
  totalPages: items.length ? 1 : 0,
});
const SESSION: InventorySessionRDto = {
  id: SESSION_ID,
  number: 'INV-0012',
  locationId: 'loc-1',
  locationCode: 'STORE',
  locationLabel: 'Boutique',
  scope: 'ALL',
  rangeId: null,
  responsibleId: 'user-1',
  responsibleName: 'Hanta Rakoto',
  countDate: '2026-11-08',
  status: 'IN_PROGRESS',
  openedAt: '2026-11-08T06:30:00Z',
  openedBy: 'user-1',
  closedAt: null,
  lineCount: 1,
  version: 0,
};
const LINE: InventoryLineRDto = {
  id: LINE_ID,
  articleId: 'article-1',
  articleCode: 'ART-0031',
  articleName: 'Savon noir 100 g',
  barcode: '3210000000031',
  unitCost: 3120.5,
  theoreticalQuantity: 12,
  countedQuantity: null,
  gap: null,
  reason: null,
};

/** INV-02 : la saisie d'une quantité met à jour immédiatement l'écart affiché, puis est enregistrée. */
describe('InventoryPage', () => {
  let fixture: ComponentFixture<InventoryPage>;
  let element: HTMLElement;
  let httpMock: HttpTestingController;

  const load = async (sessions: InventorySessionRDto[]): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [InventoryPage],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
        { provide: ConnectivityService, useValue: { isOnline: signal(true).asReadonly() } },
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(InventoryPage);
    element = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    httpMock.expectOne((req) => req.url === INVENTORIES_URL).flush(page(sessions));
    if (sessions.length) {
      httpMock
        .expectOne((req) => req.url === `${INVENTORIES_URL}/${SESSION_ID}/lines`)
        .flush(page([LINE]));
    }
    await fixture.whenStable();
    fixture.detectChanges();
  };

  const countInput = (): HTMLInputElement =>
    element.querySelector<HTMLInputElement>('#count-article-1') as HTMLInputElement;
  const gapCell = (): string => element.querySelector('td.gap')?.textContent?.trim() ?? '';
  const theoreticalCell = (): string =>
    element.querySelector('tbody td.cell-num')?.textContent?.trim() ?? '';

  const type = async (value: string, commit: boolean): Promise<void> => {
    countInput().value = value;
    countInput().dispatchEvent(new Event('input'));
    if (commit) {
      countInput().dispatchEvent(new Event('change'));
    }
    await fixture.whenStable();
    fixture.detectChanges();
  };

  afterEach(() => httpMock.verify());

  it('invite à ouvrir une session quand aucune n’est en cours', async () => {
    await load([]);

    expect(element.textContent).toContain("Aucune session d'inventaire en cours");
    expect(element.querySelector('a[href="/inventory/new"]')).not.toBeNull();
  });

  it('affiche la session en cours avec le théorique du serveur', async () => {
    await load([SESSION]);

    expect(element.querySelector('h1')?.textContent).toContain('Inventaire · Boutique');
    expect(element.textContent).toContain('Hanta Rakoto');
    expect(theoreticalCell()).toBe('12');
    expect(gapCell()).toBe('—');
  });

  it('met à jour l’écart dès la saisie, avant tout enregistrement', async () => {
    await load([SESSION]);

    await type('10', false);

    expect(gapCell()).toBe('−2');
    expect(element.querySelector('td.gap [data-sign]')?.getAttribute('data-sign')).toBe('minus');
    httpMock.expectNone(COUNT_URL);
  });

  it('enregistre la quantité validée et reprend le théorique figé par le serveur', async () => {
    await load([SESSION]);

    await type('10', true);

    const request = await vi.waitFor(() => httpMock.expectOne(COUNT_URL));
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ countedQuantity: 10 });
    request.flush({ ...LINE, theoreticalQuantity: 11, countedQuantity: 10, gap: -1 });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(theoreticalCell()).toBe('11');
    expect(gapCell()).toBe('−1');
  });

  it('accepte une quantité décimale et un écart nul', async () => {
    await load([SESSION]);

    await type('12.000', false);

    expect(gapCell()).toBe('0');
  });

  it('n’envoie rien quand le champ est vidé', async () => {
    await load([SESSION]);

    await type('', true);

    httpMock.expectNone(COUNT_URL);
    expect(gapCell()).toBe('—');
  });

  it('prévient quand le serveur refuse le comptage (session clôturée)', async () => {
    await load([SESSION]);
    const show = vi.spyOn(TestBed.inject(ToastService), 'show');

    await type('10', true);

    const request = await vi.waitFor(() => httpMock.expectOne(COUNT_URL));
    request.flush(
      { status: 409, code: 'CONFLICT', detail: 'La session INV-0012 n’est plus en cours.' },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();
    expect(show).toHaveBeenCalledWith(
      'inventory.countFailed',
      expect.objectContaining({ article: 'ART-0031' }),
      'danger',
    );
  });

  it('ne présente aucune violation d’accessibilité détectable', async () => {
    await load([SESSION]);
    document.body.appendChild(element);

    const results = await axe.run(element, {
      runOnly: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'],
      rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
    });

    expect(results.violations.map((violation) => violation.id)).toEqual([]);
    element.remove();
  });
});
