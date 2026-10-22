import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTestTranslations } from '../i18n/i18n-testing';
import { ConnectivityService } from './connectivity-service';
import { OutboxService } from './outbox-service';
import { SyncStatus } from './sync-status';

describe('SyncStatus', () => {
  const online = signal(true);
  const pending = signal(0);
  const failed = signal(0);

  async function render(): Promise<HTMLElement> {
    await TestBed.configureTestingModule({
      imports: [SyncStatus],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTestTranslations(),
        { provide: ConnectivityService, useValue: { isOnline: online.asReadonly() } },
        {
          provide: OutboxService,
          useValue: { pendingCount: pending.asReadonly(), failedCount: failed.asReadonly() },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(SyncStatus);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    online.set(true);
    pending.set(0);
    failed.set(0);
  });

  it('should confirm that everything is synchronised when online with nothing pending', async () => {
    const element = await render();

    expect(element.querySelector('[role="status"]')?.textContent).toContain('Synchronisé');
    expect(element.querySelector('a')).toBeNull();
  });

  it('should warn that the user is offline', async () => {
    online.set(false);

    const element = await render();

    expect(element.textContent).toContain('Hors ligne');
  });

  it('should show the number of pending and refused entries', async () => {
    pending.set(3);
    failed.set(1);

    const element = await render();

    expect(element.textContent).toContain('3 saisie(s) en attente');
    expect(element.textContent).toContain('1 saisie(s) refusée(s)');
  });
});
