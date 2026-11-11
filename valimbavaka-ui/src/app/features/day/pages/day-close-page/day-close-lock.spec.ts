import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { ApiError } from '../../../../core/http/problem-detail';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { OutboxService } from '../../../../core/offline/outbox-service';
import { DayClosingRDto } from '../../data-access/day-models';
import { DayClosePage } from './day-close-page';

const URL = '/api/v1/days/closing';

const LINE = {
  opening: 0,
  transferred: 10,
  returned: 0,
  delivered: 0,
  salon: 0,
  shooting: 0,
  gap: 0,
};

/** Réconciliation d'une boutique : le savon a un écart de −2 (14 attendus, 12 comptés), l'huile aucun. */
function closing(status: 'OPEN' | 'CLOSED', saved: boolean): DayClosingRDto {
  return {
    businessDayId: 'day-id',
    businessDate: '2026-11-11',
    status,
    locations: [
      {
        locationId: 'store-id',
        code: 'STORE',
        label: 'Boutique',
        lines: [
          {
            ...LINE,
            articleId: 'soap',
            reference: 'ART-0031',
            name: 'Savon noir',
            opening: 4,
            transferred: 30,
            returned: 1,
            sold: 19,
            shooting: 2,
            theoretical: 14,
            systemStock: 12,
            counted: saved ? 12 : null,
            reason: saved ? 'BREAKAGE' : null,
          },
          {
            ...LINE,
            articleId: 'oil',
            reference: 'ART-0087',
            name: 'Huile de coco',
            sold: 4,
            theoretical: 6,
            systemStock: 6,
            counted: saved ? 6 : null,
            reason: null,
          },
        ],
      },
    ],
  };
}

/** Verrouillage de la clôture (JR-06) : le serveur relit les comptages enregistrés, l'écran les exige. */
describe('DayClosePage lock', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<DayClosePage>;
  const submit = vi.fn();

  const open = async (body: DayClosingRDto): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [DayClosePage],
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideTestTranslations(),
        { provide: OutboxService, useValue: { submit } },
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DayClosePage);
    fixture.detectChanges();
    httpMock.expectOne(URL).flush(body);
    await fixture.whenStable();
    fixture.detectChanges();
  };

  const host = (): HTMLElement => fixture.nativeElement as HTMLElement;

  const lockButton = (): HTMLButtonElement | null =>
    host().querySelector<HTMLButtonElement>('.lock button.btn-primary');

  const confirmLock = async (): Promise<void> => {
    host().querySelector<HTMLButtonElement>('dialog .confirm-actions .btn-primary')?.click();
    await fixture.whenStable();
    fixture.detectChanges();
  };

  // jsdom n'a pas HTMLDialogElement.close : bouchon retiré après chaque test (prototype partagé).
  beforeEach(() => {
    submit.mockReset();
    HTMLDialogElement.prototype.close = vi.fn();
  });

  afterEach(() => {
    httpMock.verify();
    Reflect.deleteProperty(HTMLDialogElement.prototype, 'close');
  });

  it('should keep the lock disabled until the counts shown are the ones saved by the server', async () => {
    await open(closing('OPEN', false));

    expect(lockButton()?.disabled).toBe(true);
    expect(host().querySelector('.lock .hint')?.textContent).toContain('comptage est enregistré');
  });

  it('should enable the lock once every count is saved and lock the day with the server', async () => {
    await open(closing('OPEN', true));
    submit.mockReturnValue(of({ queued: false, body: closing('CLOSED', true) }));

    expect(lockButton()?.disabled).toBe(false);
    await confirmLock();

    expect(submit).toHaveBeenCalledWith('POST', '/api/v1/days/closing/lock');
    expect(host().querySelector('.locked-banner')?.textContent).toContain('Journée verrouillée à');
    expect(lockButton()).toBeNull();
    expect(host().querySelector<HTMLInputElement>('tbody input')?.disabled).toBe(true);
  });

  it('should show the refusal of the server when the lock is refused (422)', async () => {
    await open(closing('OPEN', true));
    const refusal = new ApiError(422, 'VALIDATION_FAILED', 'Un motif est obligatoire : ART-0031.');
    submit.mockReturnValue(throwError(() => refusal));

    await confirmLock();

    expect(host().querySelector('.lock [role="alert"]')?.textContent).toContain(
      'Un motif est obligatoire',
    );
    expect(host().querySelector('.locked-banner')).toBeNull();
  });

  it('should leave the day open when the lock is only queued for the network', async () => {
    await open(closing('OPEN', true));
    submit.mockReturnValue(of({ queued: true }));

    await confirmLock();

    expect(host().querySelector('.locked-banner')).toBeNull();
    expect(lockButton()?.disabled).toBe(false);
  });

  it('should show a day already closed as read only, without the lock button', async () => {
    await open(closing('CLOSED', true));

    expect(host().querySelector('.locked-banner')?.textContent).toContain('consultation seule');
    expect(lockButton()).toBeNull();
    expect(host().querySelector<HTMLInputElement>('tbody input')?.disabled).toBe(true);
  });
});
