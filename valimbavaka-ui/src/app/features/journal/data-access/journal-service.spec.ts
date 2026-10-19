import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { JournalService } from './journal-service';

describe('JournalService', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should read a page of the journal with the VVS pagination parameters', () => {
    TestBed.inject(JournalService).list({ page: 3, size: 20, search: 'Rakoto' }).subscribe();

    const request = httpMock.expectOne((req) => req.url === '/api/v1/journal');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('3');
    expect(request.request.params.get('size')).toBe('20');
    expect(request.request.params.get('search')).toBe('Rakoto');
    request.flush({ items: [], page: 3, pageSize: 20, totalCount: 0, totalPages: 0 });
  });
});
