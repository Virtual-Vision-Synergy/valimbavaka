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
const REASON_URL = `${INVENTORIES_URL}/${SESSION_ID}/lines/${LINE_ID}/reason`;
const CLOSE_URL = `${INVENTORIES_URL}/${SESSION_ID}/close`;
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
  gapCount: 0,
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

const CLOSED: InventorySessionRDto = { ...SESSION, id: 'closed-1', status: 'CLOSED', gapCount: 3 };

/** Ligne déjà comptée avec un écart de −2 (10 comptés pour 12 attendus). */
const LINE_WITH_GAP: InventoryLineRDto = { ...LINE, countedQuantity: 10, gap: -2 };

/**
 * INV-02 : la saisie d'une quantité met à jour immédiatement l'écart affiché, puis est enregistrée.
 * INV-03 : un motif est sélectionnable, et enregistré, pour toute ligne en écart.
 * INV-04 : la clôture est envoyée au serveur, qui ajuste le stock ; la feuille est alors verrouillée.
 * INV-05 : les dernières sessions clôturées sont listées avec leur nombre d'écarts et un lien vers l'historique.
 */
describe('InventoryPage', () => {
  let fixture: ComponentFixture<InventoryPage>;
  let element: HTMLElement;
  let httpMock: HttpTestingController;

  const load = async (
    sessions: InventorySessionRDto[],
    line: InventoryLineRDto = LINE,
    previous: InventorySessionRDto[] = [],
  ): Promise<void> => {
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
      httpMock.expectOne((req) => req.params.get('status') === 'CLOSED').flush(page(previous));
      httpMock
        .expectOne((req) => req.url === `${INVENTORIES_URL}/${SESSION_ID}/lines`)
        .flush(page([line]));
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

  const reasonSelect = (): HTMLSelectElement | null =>
    element.querySelector<HTMLSelectElement>('select.reason');

  const chooseReason = async (value: string): Promise<void> => {
    const select = reasonSelect() as HTMLSelectElement;
    select.value = value;
    select.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    fixture.detectChanges();
  };

  afterEach(() => httpMock.verify());

  it('invite à ouvrir une session, ou à consulter l’historique, quand aucune n’est en cours', async () => {
    await load([]);

    expect(element.textContent).toContain("Aucune session d'inventaire en cours");
    expect(element.querySelector('a[href="/inventory/new"]')).not.toBeNull();
    expect(element.querySelector('a[href="/inventory/history"]')).not.toBeNull();
  });

  it('affiche la session en cours, son théorique et les sessions précédentes (INV-05)', async () => {
    await load([SESSION], LINE, [CLOSED]);

    expect(element.querySelector('h1')?.textContent).toContain('Inventaire · Boutique');
    expect(element.textContent).toContain('Hanta Rakoto');
    expect(theoreticalCell()).toBe('12');
    expect(gapCell()).toBe('—');
    expect(element.querySelector('ul.previous')?.textContent).toContain('3 écart(s)');
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

  it('propose un motif pour une ligne en écart et l’enregistre', async () => {
    await load([SESSION], LINE_WITH_GAP);
    expect(reasonSelect()?.getAttribute('aria-invalid')).toBe('true');

    await chooseReason('BREAKAGE');

    const request = await vi.waitFor(() => httpMock.expectOne(REASON_URL));
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ reason: 'BREAKAGE' });
    request.flush({ ...LINE_WITH_GAP, reason: 'BREAKAGE' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(reasonSelect()?.value).toBe('BREAKAGE');
    expect(reasonSelect()?.getAttribute('aria-invalid')).toBe('false');
  });

  it('ne propose aucun motif pour une ligne sans écart ou non comptée', async () => {
    await load([SESSION]);
    expect(reasonSelect()).toBeNull();

    await type('12', false);

    expect(reasonSelect()).toBeNull();
  });

  it('n’envoie rien quand on revient sur « Choisir un motif… »', async () => {
    await load([SESSION], { ...LINE_WITH_GAP, reason: 'LOSS' });

    await chooseReason('');

    httpMock.expectNone(REASON_URL);
    expect(reasonSelect()?.getAttribute('aria-invalid')).toBe('true');
  });

  it('prévient quand le serveur refuse le motif', async () => {
    await load([SESSION], LINE_WITH_GAP);
    const show = vi.spyOn(TestBed.inject(ToastService), 'show');

    await chooseReason('LOSS');

    const request = await vi.waitFor(() => httpMock.expectOne(REASON_URL));
    request.flush(
      { status: 422, code: 'VALIDATION_FAILED', detail: 'La ligne ART-0031 n’est pas en écart.' },
      { status: 422, statusText: 'Unprocessable Content' },
    );
    await fixture.whenStable();
    expect(show).toHaveBeenCalledWith(
      'inventory.reasonFailed',
      expect.objectContaining({ article: 'ART-0031' }),
      'danger',
    );
  });

  it('reprend le motif effacé par le serveur après un recomptage sans écart', async () => {
    await load([SESSION], { ...LINE_WITH_GAP, reason: 'BREAKAGE' });

    await type('12', true);
    const request = await vi.waitFor(() => httpMock.expectOne(COUNT_URL));
    request.flush({ ...LINE, countedQuantity: 12, gap: 0, reason: null });
    await fixture.whenStable();
    fixture.detectChanges();
    await type('10', false);

    expect(reasonSelect()?.value).toBe('');
  });

  describe('clôture (INV-04)', () => {
    /** Ligne comptée en écart et expliquée : la session peut être clôturée. */
    const READY_LINE: InventoryLineRDto = { ...LINE_WITH_GAP, reason: 'BREAKAGE' };

    const closeButton = (): HTMLButtonElement =>
      element.querySelector<HTMLButtonElement>('.close button') as HTMLButtonElement;

    const confirm = async (): Promise<void> => {
      element.querySelector<HTMLButtonElement>('dialog .confirm-actions .btn-primary')?.click();
      await fixture.whenStable();
    };

    // jsdom n'a pas HTMLDialogElement.close : bouchon retiré après chaque test (prototype partagé).
    beforeEach(() => {
      HTMLDialogElement.prototype.close = vi.fn();
    });
    afterEach(() => Reflect.deleteProperty(HTMLDialogElement.prototype, 'close'));

    it('n’autorise pas la clôture tant qu’un écart n’a pas de motif', async () => {
      await load([SESSION], LINE_WITH_GAP);

      expect(closeButton().disabled).toBe(true);
      expect(element.querySelector('#close-hint')?.textContent).toContain('chaque écart expliqué');
    });

    it('clôture la session sur le serveur puis verrouille la feuille', async () => {
      await load([SESSION], READY_LINE);
      const show = vi.spyOn(TestBed.inject(ToastService), 'show');
      expect(closeButton().disabled).toBe(false);

      await confirm();

      const request = await vi.waitFor(() => httpMock.expectOne(CLOSE_URL));
      expect(request.request.method).toBe('POST');
      request.flush({ ...SESSION, status: 'CLOSED', closedAt: '2026-11-08T14:30:00Z' });
      await fixture.whenStable();
      fixture.detectChanges();
      expect(show).toHaveBeenCalledWith('inventory.closedToast', { number: 'INV-0012' });
      expect(element.querySelector('h1')?.textContent).toContain('Clôturée');
      expect(element.querySelector('.done')?.textContent).toContain('Session clôturée à');
      expect(countInput().disabled).toBe(true);
      expect(reasonSelect()?.disabled).toBe(true);
    });

    it('prévient et laisse la feuille ouverte quand le serveur refuse la clôture', async () => {
      await load([SESSION], READY_LINE);
      const show = vi.spyOn(TestBed.inject(ToastService), 'show');

      await confirm();

      const request = await vi.waitFor(() => httpMock.expectOne(CLOSE_URL));
      request.flush(
        {
          status: 422,
          code: 'VALIDATION_FAILED',
          detail: 'Clôture impossible : écart(s) sans motif ART-0031 (1).',
        },
        { status: 422, statusText: 'Unprocessable Content' },
      );
      await fixture.whenStable();
      fixture.detectChanges();
      expect(show).toHaveBeenCalledWith(
        'inventory.closeFailed',
        expect.objectContaining({ number: 'INV-0012' }),
        'danger',
      );
      expect(countInput().disabled).toBe(false);
      expect(closeButton().disabled).toBe(false);
    });
  });

  it('ne présente aucune violation d’accessibilité détectable', async () => {
    await load([SESSION], LINE_WITH_GAP, [CLOSED]);
    document.body.appendChild(element);

    const results = await axe.run(element, {
      runOnly: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'],
      rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
    });

    expect(results.violations.map((violation) => violation.id)).toEqual([]);
    element.remove();
  });
});
