import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import axe from 'axe-core';
import { provideTestTranslations } from '../i18n/i18n-testing';
import { ConnectivityService } from './connectivity-service';
import { OutboxService } from './outbox-service';
import { SyncStatus } from './sync-status';

/** Contrôle axe (WCAG 2.2 AA) de l'indicateur dans son état le plus chargé ; le contraste exige un vrai navigateur. */
describe('SyncStatus accessibility', () => {
  it('should have no detectable accessibility violation', async () => {
    await TestBed.configureTestingModule({
      imports: [SyncStatus],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTestTranslations(),
        { provide: ConnectivityService, useValue: { isOnline: signal(false).asReadonly() } },
        {
          provide: OutboxService,
          useValue: { pendingCount: signal(2).asReadonly(), failedCount: signal(1).asReadonly() },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(SyncStatus);
    await fixture.whenStable();
    document.body.appendChild(fixture.nativeElement as HTMLElement);

    const results = await axe.run(fixture.nativeElement as HTMLElement, {
      runOnly: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'],
      rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
    });

    expect(results.violations.map((violation) => violation.id)).toEqual([]);
  });
});
