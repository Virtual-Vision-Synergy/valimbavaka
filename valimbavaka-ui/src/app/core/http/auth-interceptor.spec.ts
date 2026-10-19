import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthStore } from '../auth/auth-store';
import { buildUser } from '../auth/auth-testing';
import { authInterceptor } from './auth-interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthStore).signIn('jeton-acces', buildUser('ADMIN'));
  });

  afterEach(() => httpMock.verify());

  it('should add the bearer token to calls to our API', () => {
    http.get('/api/v1/users').subscribe();

    const request = httpMock.expectOne('/api/v1/users');
    expect(request.request.headers.get('Authorization')).toBe('Bearer jeton-acces');
    request.flush({});
  });

  it('should never send the token to a third-party domain', () => {
    http.get('https://cdn.example.com/data.json').subscribe();

    const request = httpMock.expectOne('https://cdn.example.com/data.json');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });
});
