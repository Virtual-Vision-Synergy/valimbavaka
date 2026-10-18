import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { errorInterceptor } from './error-interceptor';
import { ApiError } from './problem-detail';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should convert a ProblemDetail into a typed ApiError with field errors', () => {
    let received: unknown;
    http.post('/api/v1/users', {}).subscribe({ error: (error: unknown) => (received = error) });

    httpMock.expectOne('/api/v1/users').flush(
      {
        status: 422,
        code: 'VALIDATION_FAILED',
        detail: '1 champ(s) invalide(s)',
        errors: [{ field: 'email', message: 'doit être une adresse email valide' }],
      },
      { status: 422, statusText: 'Unprocessable Content' },
    );

    expect(received).toBeInstanceOf(ApiError);
    const apiError = received as ApiError;
    expect(apiError.code).toBe('VALIDATION_FAILED');
    expect(apiError.message).toBe('1 champ(s) invalide(s)');
    expect(apiError.fieldErrors).toEqual([
      { field: 'email', message: 'doit être une adresse email valide' },
    ]);
  });

  it('should fall back to a generic message when the body is not a ProblemDetail', () => {
    let received: unknown;
    http.get('/api/v1/users').subscribe({ error: (error: unknown) => (received = error) });

    httpMock
      .expectOne('/api/v1/users')
      .flush('Bad Gateway', { status: 502, statusText: 'Bad Gateway' });

    expect((received as ApiError).status).toBe(502);
    expect((received as ApiError).message).toBe('Une erreur inattendue est survenue');
  });
});
