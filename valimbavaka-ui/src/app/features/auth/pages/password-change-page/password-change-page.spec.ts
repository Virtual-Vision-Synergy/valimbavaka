import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { PasswordChangePage } from './password-change-page';

describe('PasswordChangePage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<PasswordChangePage>;
  let element: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PasswordChangePage],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTestTranslations(),
      ],
    }).compileComponents();
    TestBed.inject(AuthStore).signIn('provisoire', {
      ...buildUser('ADMIN'),
      permissions: [],
      passwordChangeRequired: true,
    });
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PasswordChangePage);
    element = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  const type = (id: string, value: string): void => {
    const input = element.querySelector<HTMLInputElement>(`#${id}`);
    if (!input) {
      throw new Error(`champ ${id} absent`);
    }
    input.value = value;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
  };

  const submit = async (): Promise<void> => {
    element.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();
    await fixture.whenStable();
  };

  it('should not call the server when the new password is shorter than 15 characters', async () => {
    type('current-password', 'mot-de-passe-provisoire');
    type('new-password', 'court-14-carac');
    type('confirmation', 'court-14-carac');

    await submit();

    httpMock.expectNone('/api/v1/auth/password');
    expect(element.textContent).toContain('au moins 15 caractères');
  });

  it('should not call the server when the confirmation differs', async () => {
    type('current-password', 'mot-de-passe-provisoire');
    type('new-password', 'nouveau-mot-de-passe-choisi');
    type('confirmation', 'autre-mot-de-passe-choisi');

    await submit();

    httpMock.expectNone('/api/v1/auth/password');
    expect(element.textContent).toContain('ne correspondent pas');
  });

  it('should change the password, renew the session and go to the home page', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    type('current-password', 'mot-de-passe-provisoire');
    type('new-password', 'nouveau-mot-de-passe-choisi');
    type('confirmation', 'nouveau-mot-de-passe-choisi');

    await submit();
    httpMock
      .expectOne('/api/v1/auth/password')
      .flush(null, { status: 204, statusText: 'No Content' });
    httpMock.expectOne('/api/v1/auth/refresh').flush({
      accessToken: 'jeton',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: buildUser('ADMIN'),
    });
    await fixture.whenStable();

    expect(navigate).toHaveBeenCalledWith('/');
    expect(TestBed.inject(AuthStore).currentUser()?.passwordChangeRequired).toBe(false);
  });

  it('should display the server message when the current password is wrong', async () => {
    type('current-password', 'pas-le-bon-mot-de-passe');
    type('new-password', 'nouveau-mot-de-passe-choisi');
    type('confirmation', 'nouveau-mot-de-passe-choisi');

    await submit();
    httpMock.expectOne('/api/v1/auth/password').flush(
      {
        title: 'Unprocessable Content',
        status: 422,
        detail: 'Le mot de passe actuel est incorrect.',
        code: 'VALIDATION_FAILED',
      },
      { status: 422, statusText: 'Unprocessable Content' },
    );
    await fixture.whenStable();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Le mot de passe actuel est incorrect.',
    );
  });
});
