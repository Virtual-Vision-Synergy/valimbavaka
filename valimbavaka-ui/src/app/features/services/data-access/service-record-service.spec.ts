import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { AuthStore } from '../../../core/auth/auth-store';
import { buildUser } from '../../../core/auth/auth-testing';
import { ServiceRecordWDto } from './service-record-models';
import { ServiceRecordService } from './service-record-service';

const RECORDS_URL = '/api/v1/service-records';
const EMPTY_PAGE = { items: [], page: 1, pageSize: 100, totalCount: 0, totalPages: 0 };

describe('ServiceRecordService', () => {
  let service: ServiceRecordService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    TestBed.inject(AuthStore).signIn('jeton', buildUser('SELLER')); // l'écriture passe par la file hors ligne
    service = TestBed.inject(ServiceRecordService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lit les prestations les plus récentes avec la pagination demandée', () => {
    service.list({ page: 1, size: 100, sortBy: 'createdAt', sortDirection: 'DESC' }).subscribe();

    const request = httpMock.expectOne((req) => req.url === RECORDS_URL);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('size')).toBe('100');
    expect(request.request.params.get('sortDirection')).toBe('DESC');
    request.flush(EMPTY_PAGE);
  });

  it('enregistre une prestation par POST avec le service, la cliente et les produits consommés', async () => {
    const body: ServiceRecordWDto = {
      salonServiceId: 'soin-visage',
      customerName: 'Hanta',
      products: [{ articleId: 'huile-coco', quantity: 0.015 }],
    };

    const result = firstValueFrom(service.create(body));

    const request = httpMock.expectOne(RECORDS_URL);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    expect(request.request.headers.has('Idempotency-Key')).toBe(true);
    request.flush({ id: 'p1', number: 'PRE-0001' });
    expect((await result).queued).toBe(false);
  });
});
