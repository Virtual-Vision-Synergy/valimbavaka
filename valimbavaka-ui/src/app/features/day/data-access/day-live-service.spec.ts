import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DayLiveRDto } from './day-live-models';
import { DayLiveService } from './day-live-service';

const LIVE: DayLiveRDto = {
  businessDate: '2026-11-11',
  status: 'OPEN',
  generatedAt: '2026-11-11T10:00:00Z',
  sales: { revenue: 17000, count: 2, channels: [] },
  deliveries: { total: 3, toPrepare: 1, onRound: 1, delivered: 1, failed: 0, cancelled: 0 },
  services: { count: 1, revenue: 35000 },
  feed: [],
};

describe('DayLiveService', () => {
  let httpMock: HttpTestingController;
  let service: DayLiveService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    service = TestBed.inject(DayLiveService);
  });

  afterEach(() => httpMock.verify());

  it('should read the live view of the day from the API', () => {
    let received: DayLiveRDto | undefined;

    service.live().subscribe((view) => (received = view));
    const request = httpMock.expectOne('/api/v1/days/live');
    request.flush(LIVE);

    expect(request.request.method).toBe('GET');
    expect(received).toEqual(LIVE);
  });
});
