import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthStore } from '../../../../core/auth/auth-store';
import { buildUser } from '../../../../core/auth/auth-testing';
import { errorInterceptor } from '../../../../core/http/error-interceptor';
import { provideTestTranslations } from '../../../../core/i18n/i18n-testing';
import { LoginPage } from './login-page';

describe('LoginPage', () => {
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<LoginPage>;
  let element: HTMLElement;

  const type = (selector: string, value: string): void => {
    const input = element.querySelector<HTMLInputElement>(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  };

  const submit = (): void => {
    element.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideTestTranslations(),
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(LoginPage);
    await fixture.whenStable();
    element = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => httpMock.verify());

  it('should send only the email and password, never a role', async () => {
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    type('#email', 'hanta@valimbavaka.mg');
    type('#password', 'mot-de-passe-long');

    submit();

    const request = httpMock.expectOne('/api/v1/auth/login');
    expect(request.request.body).toEqual({
      email: 'hanta@valimbavaka.mg',
      password: 'mot-de-passe-long',
    });
    request.flush({
      accessToken: 'jeton',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: buildUser('SELLER'),
    });
    expect(TestBed.inject(AuthStore).currentUser()?.role).toBe('SELLER');
  });

  it('should show a generic message when the credentials are rejected', async () => {
    type('#email', 'hanta@valimbavaka.mg');
    type('#password', 'mauvais');

    submit();
    httpMock
      .expectOne('/api/v1/auth/login')
      .flush(
        { status: 401, code: 'UNAUTHENTICATED', detail: 'Identifiants invalides' },
        { status: 401, statusText: 'Unauthorized' },
      );
    await fixture.whenStable();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Identifiants invalides',
    );
    expect(TestBed.inject(AuthStore).isAuthenticated()).toBe(false);
  });

  it('should not call the server while the form is invalid', async () => {
    submit();

    httpMock.expectNone('/api/v1/auth/login');
  });
});
