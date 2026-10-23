import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { PageResponse } from '../../../../shared/models/page-response';
import { UserRDto } from '../../data-access/user-models';
import { UserListPage } from './user-list-page';

const user = (overrides: Partial<UserRDto> = {}): UserRDto => ({
  id: '0195f0a0-0000-7000-8000-0000000000a1',
  email: 'hery@valimbavaka.mg',
  firstName: 'Hery',
  lastName: 'Rabe',
  role: { id: '1', name: 'SELLER', description: 'Vendeuse' },
  salesChannelId: null,
  commissionRate: null,
  active: true,
  passwordChangeRequired: false,
  createdAt: '2026-10-19T08:30:00Z',
  updatedAt: '2026-10-19T08:30:00Z',
  ...overrides,
});

const pageOf = (items: UserRDto[]): PageResponse<UserRDto> => ({
  items,
  page: 1,
  pageSize: 10,
  totalCount: items.length,
  totalPages: 1,
});

describe('UserListPage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<UserListPage>;
  let element: HTMLElement;

  const isListRequest = (req: { url: string }): boolean => req.url === '/api/v1/users';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserListPage],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('jeton', buildUser('ADMIN'));
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(UserListPage);
    element = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  it('should show an ACTIF badge for an active user and DÉSACTIVÉ for a disabled one', async () => {
    httpMock
      .expectOne(isListRequest)
      .flush(pageOf([user(), user({ id: 'b', email: 'tiana@valimbavaka.mg', active: false })]));
    await fixture.whenStable();

    const rows = element.querySelectorAll('tbody tr');
    expect(rows[0].textContent).toContain('ACTIF');
    expect(rows[1].textContent).toContain('DÉSACTIVÉ');
  });

  it('should offer no way to delete an account, only to deactivate it', async () => {
    httpMock.expectOne(isListRequest).flush(pageOf([user()]));
    await fixture.whenStable();

    const labels = [...element.querySelectorAll('tbody button')].map((button) =>
      button.textContent?.trim(),
    );
    expect(labels).toEqual(['Désactiver']);
  });

  it('should deactivate an active user after confirmation and reload the list', async () => {
    httpMock.expectOne(isListRequest).flush(pageOf([user()]));
    await fixture.whenStable();

    toggleButton().click();
    await confirmDialog();

    const patch = httpMock.expectOne('/api/v1/users/0195f0a0-0000-7000-8000-0000000000a1/status');
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({ active: false });
    patch.flush(user({ active: false }));
    httpMock.expectOne(isListRequest).flush(pageOf([user({ active: false })]));
    await fixture.whenStable();

    expect(element.querySelector('tbody tr')?.textContent).toContain('DÉSACTIVÉ');
  });

  it('should not call the API when the confirmation is declined', async () => {
    httpMock.expectOne(isListRequest).flush(pageOf([user()]));
    await fixture.whenStable();

    toggleButton().click();
    await fixture.whenStable();
    dialogButton(/Annuler/).click();
    await fixture.whenStable();

    httpMock.expectNone('/api/v1/users/0195f0a0-0000-7000-8000-0000000000a1/status');
  });

  it('should show the server error when the status change is refused', async () => {
    httpMock.expectOne(isListRequest).flush(pageOf([user()]));
    await fixture.whenStable();

    toggleButton().click();
    await confirmDialog();
    httpMock.expectOne('/api/v1/users/0195f0a0-0000-7000-8000-0000000000a1/status').flush(
      {
        status: 422,
        code: 'VALIDATION_FAILED',
        detail: 'Vous ne pouvez pas désactiver votre propre compte.',
      },
      { status: 422, statusText: 'Unprocessable Entity' },
    );
    await fixture.whenStable();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('propre compte');
  });

  function dialogButton(label: RegExp): HTMLButtonElement {
    const button = Array.from(element.querySelectorAll<HTMLButtonElement>('dialog button')).find(
      (candidate) => label.test(candidate.textContent ?? ''),
    );
    if (!button) {
      throw new Error('dialog button not found');
    }
    return button;
  }

  /** Ouvre le dialogue de confirmation puis clique sur son bouton de validation. */
  async function confirmDialog(): Promise<void> {
    await fixture.whenStable();
    dialogButton(/Désactiver|Activer/).click();
  }

  function toggleButton(): HTMLButtonElement {
    const button = Array.from(element.querySelectorAll<HTMLButtonElement>('tbody button')).find(
      (candidate) => /Désactiver|Activer/.test(candidate.textContent ?? ''),
    );
    if (!button) {
      throw new Error('toggle button not found');
    }
    return button;
  }
});
