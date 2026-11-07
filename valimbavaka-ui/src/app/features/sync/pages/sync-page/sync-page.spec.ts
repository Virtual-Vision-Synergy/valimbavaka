import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { ConnectivityService } from '../../../../core/offline/connectivity-service';
import { OutboxEntry } from '../../../../core/offline/outbox-models';
import { OutboxService } from '../../../../core/offline/outbox-service';
import { ToastService } from '../../../../shared/components/toast/toast-service';
import { SyncPage } from './sync-page';

describe('SyncPage', () => {
  const failed = signal<OutboxEntry[]>([]);
  const pending = signal<OutboxEntry[]>([]);
  const online = signal(true);
  const syncing = signal(false);
  const retry = vi.fn();
  const discard = vi.fn();
  const show = vi.fn();

  const entry = (overrides: Partial<OutboxEntry> = {}): OutboxEntry => ({
    id: 'e1',
    method: 'PUT',
    url: '/api/v1/suppliers/0195f0a0-0000-7000-8000-0000000000b1',
    body: {},
    createdAt: Date.UTC(2026, 10, 7, 9, 30),
    attempts: 1,
    status: 'FAILED',
    errorMessage: 'La fiche a été modifiée par un autre utilisateur.',
    ...overrides,
  });

  async function renderFixture(): Promise<ComponentFixture<SyncPage>> {
    await TestBed.configureTestingModule({
      imports: [SyncPage],
      providers: [
        provideTestTranslations(),
        { provide: ConnectivityService, useValue: { isOnline: online.asReadonly() } },
        { provide: ToastService, useValue: { show } },
        {
          provide: OutboxService,
          useValue: {
            failed: failed.asReadonly(),
            pending: pending.asReadonly(),
            isSyncing: syncing.asReadonly(),
            retry,
            discard,
          },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(SyncPage);
    await fixture.whenStable();
    return fixture;
  }

  async function render(): Promise<HTMLElement> {
    return (await renderFixture()).nativeElement as HTMLElement;
  }

  const button = (element: HTMLElement, selector: string, label: string): HTMLButtonElement => {
    const found = Array.from(element.querySelectorAll<HTMLButtonElement>(selector)).find((b) =>
      b.textContent?.includes(label),
    );
    if (!found) {
      throw new Error(`button « ${label} » not found`);
    }
    return found;
  };

  beforeEach(() => {
    failed.set([]);
    pending.set([]);
    online.set(true);
    syncing.set(false);
    retry.mockReset().mockResolvedValue(undefined);
    discard.mockReset().mockResolvedValue(undefined);
    show.mockReset();
  });

  it('should tell the user that everything is synchronised when nothing failed', async () => {
    const element = await render();

    expect(element.textContent).toContain('tout est synchronisé');
    expect(element.querySelector('table')).toBeNull();
  });

  it('should show how many entries are pending and refused', async () => {
    failed.set([entry()]);
    pending.set([entry({ id: 'p1', status: 'PENDING' }), entry({ id: 'p2', status: 'PENDING' })]);

    const element = await render();

    expect(element.querySelector('header')?.textContent).toContain('2 saisie(s) en attente');
    expect(element.querySelector('header')?.textContent).toContain('1 saisie(s) refusée(s)');
  });

  it('should list each refused entry with its action, address and reason', async () => {
    failed.set([entry()]);

    const element = await render();

    expect(element.textContent).toContain('Modification');
    expect(element.textContent).toContain('/api/v1/suppliers/0195f0a0-0000-7000-8000-0000000000b1');
    expect(element.textContent).toContain('La fiche a été modifiée par un autre utilisateur.');
  });

  it('should explain that no reason was given when the server sent none', async () => {
    failed.set([entry({ errorMessage: undefined })]);

    const element = await render();

    expect(element.textContent).toContain('Motif non communiqué par le serveur.');
  });

  it('should list pending entries read-only with their last transient error', async () => {
    pending.set([entry({ id: 'p1', status: 'PENDING', errorMessage: 'Serveur indisponible.' })]);

    const element = await render();

    expect(element.textContent).toContain("Saisies en attente d'envoi");
    expect(element.textContent).toContain('Dernière tentative : Serveur indisponible.');
    expect(element.querySelectorAll('tbody button')).toHaveLength(0);
  });

  it('should resend a refused entry and confirm it to the user', async () => {
    failed.set([entry()]);
    const element = await render();

    button(element, 'tbody button', 'Renvoyer').click();
    await vi.waitFor(() => expect(show).toHaveBeenCalled());

    expect(retry).toHaveBeenCalledWith('e1');
    expect(show).toHaveBeenCalledWith('sync.page.retried', {}, 'info');
  });

  it('should warn that a resent entry waits for the network when offline', async () => {
    online.set(false);
    failed.set([entry()]);
    const element = await render();

    button(element, 'tbody button', 'Renvoyer').click();
    await vi.waitFor(() => expect(show).toHaveBeenCalled());

    expect(show).toHaveBeenCalledWith('sync.page.retriedOffline', {}, 'info');
  });

  it('should not resend while a synchronisation is running', async () => {
    syncing.set(true);
    failed.set([entry()]);

    const element = await render();

    expect(button(element, 'tbody button', 'Renvoyer').disabled).toBe(true);
  });

  it('should ask for confirmation before abandoning an entry', async () => {
    failed.set([entry()]);
    const fixture = await renderFixture();
    const element = fixture.nativeElement as HTMLElement;

    button(element, 'tbody button', 'Abandonner').click();
    await fixture.whenStable();

    expect(element.querySelector('dialog')?.textContent).toContain('Abandonner cette saisie ?');
    expect(discard).not.toHaveBeenCalled();
  });

  it('should drop an entry once the user confirms the abandon', async () => {
    failed.set([entry()]);
    const fixture = await renderFixture();
    const element = fixture.nativeElement as HTMLElement;

    button(element, 'tbody button', 'Abandonner').click();
    await fixture.whenStable();
    button(element, 'dialog button', 'Abandonner').click();
    await vi.waitFor(() => expect(show).toHaveBeenCalledWith('sync.page.discarded'));

    expect(discard).toHaveBeenCalledWith('e1');
  });

  it('should keep the entry when the user cancels the abandon', async () => {
    failed.set([entry()]);
    const fixture = await renderFixture();
    const element = fixture.nativeElement as HTMLElement;

    button(element, 'tbody button', 'Abandonner').click();
    await fixture.whenStable();
    button(element, 'dialog button', 'Annuler').click();
    await fixture.whenStable();

    expect(discard).not.toHaveBeenCalled();
    expect(element.querySelector('dialog')).toBeNull();
  });
});
