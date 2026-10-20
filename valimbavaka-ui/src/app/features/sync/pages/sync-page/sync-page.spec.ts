import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { OutboxEntry } from '../../../../core/offline/outbox-models';
import { OutboxService } from '../../../../core/offline/outbox-service';
import { SyncPage } from './sync-page';

describe('SyncPage', () => {
  const failed = signal<OutboxEntry[]>([]);
  const retry = vi.fn();
  const discard = vi.fn();

  const entry = (overrides: Partial<OutboxEntry> = {}): OutboxEntry => ({
    id: 'e1',
    method: 'POST',
    url: '/api/v1/users',
    body: {},
    createdAt: 1,
    attempts: 1,
    status: 'FAILED',
    errorMessage: 'Email déjà utilisé',
    ...overrides,
  });

  async function render(): Promise<HTMLElement> {
    await TestBed.configureTestingModule({
      imports: [SyncPage],
      providers: [
        provideTestTranslations(),
        {
          provide: OutboxService,
          useValue: {
            failed: failed.asReadonly(),
            pendingCount: signal(0).asReadonly(),
            isSyncing: signal(false).asReadonly(),
            retry,
            discard,
          },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(SyncPage);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  const button = (element: HTMLElement, label: string): HTMLButtonElement =>
    Array.from(element.querySelectorAll('button')).find((b) => b.textContent?.includes(label))!;

  beforeEach(() => {
    failed.set([]);
    retry.mockReset();
    discard.mockReset();
  });

  it('should tell the user that everything is synchronised when nothing failed', async () => {
    const element = await render();

    expect(element.textContent).toContain('tout est synchronisé');
    expect(element.querySelector('table')).toBeNull();
  });

  it('should list each refused entry with its reason', async () => {
    failed.set([entry()]);

    const element = await render();

    expect(element.textContent).toContain('POST /api/v1/users');
    expect(element.textContent).toContain('Email déjà utilisé');
  });

  it('should resend an entry when the user asks to', async () => {
    failed.set([entry()]);
    const element = await render();

    button(element, 'Renvoyer').click();

    expect(retry).toHaveBeenCalledWith('e1');
  });

  it('should drop an entry when the user abandons it', async () => {
    failed.set([entry()]);
    const element = await render();

    button(element, 'Abandonner').click();

    expect(discard).toHaveBeenCalledWith('e1');
  });
});
